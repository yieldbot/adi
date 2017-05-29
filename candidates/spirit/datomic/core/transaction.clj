(ns spirit.core.transaction
  (:require [hara.common
             [checks :refer [hash-map? long?]]
             [error :refer [error]]]
            [hara.data
             [map :refer [assoc-nil]]
             [nested :refer [merge-nil-nested
                             merge-nested]]]
            [spirit.data
             [common :refer [iid-seed]]
             [checks :refer [db-id?]]]
            [spirit.core
             [prepare :as prepare]
             [select :as select]
             [model :as model]
             [link :as link]
             [depack :as depack]]
            [spirit.process
             [normalise :as normalise]
             [pack :as pack]
             [unpack :as unpack]
             [emit :as emit]]
            [hara.event :refer [raise]]
            [datomic.api :as datomic]
            [clojure.walk :as walk]))

(defn gen-datoms [spirit]
  (-> spirit
      (assoc :type "datoms")
      (normalise/normalise)
      (pack/pack)
      (emit/emit)))

(defn wrap-datom-vector [f]
  (fn [spirit]
    (let [data (-> spirit :process :input)]
      (if (vector? data)
        ((select/wrap-merge-results f mapcat) spirit)
        (f spirit)))))

(defn has-hash? [m]
  (and (hash-map? m)
       (:# m)))

(defn replace-hash [m ids]
  (let [id (-> m :# :id)
        id-num (cond (symbol? id) (get ids id)
                     (db-id? id) (get ids (iid-seed id))
                     :else id)]
    (-> m
        (dissoc :#)
        (assoc-in [:db :id] id-num))))

(defn replace-ids [form ids]
  (walk/prewalk
   (fn [form]
     (cond (has-hash? form) (replace-hash form ids)
           (db-id? form) (get ids (iid-seed form))
           :else form))
   form))

(defn resolve-tempids [spirit result]
  (let [ids (->> (-> spirit :tempids deref)
                 (map #(vector (iid-seed %)
                               (datomic/resolve-tempid (datomic/db (:connection spirit))
                                                       (:tempids result)
                                                       %)))
                 (into {}))]
    (depack/depack (replace-ids (-> spirit :process :reviewed) ids)
                   spirit)))

(defn wrap-transact-options [f]
  (fn [spirit]
    (let [spirit (f spirit)
          opt-trans (-> spirit :transact)
          result (cond (#{:async :promise} opt-trans)
                       (-> spirit :result :promise)

                       :else
                       (let [transact (or (-> spirit :result :simulation)
                                          @(-> spirit :result :promise))]
                         (cond (= :datomic opt-trans) transact

                               (or (nil? opt-trans)
                                   (= :resolve opt-trans))
                               (resolve-tempids spirit transact)

                               :else
                               (error "WRAP_TRANSACT_OPTIONS: #{:datomic :async :promise :resolve(default)}"))))]
      (assoc-in spirit [:result :transact] result))))

(defn wrap-transact-results [f]
  (fn [spirit]
    (let [spirit (f spirit)
          opt-trans (-> spirit :transact)]

      (cond (-> spirit :options :spirit)
            spirit

            (or (nil? opt-trans)
                (#{:resolve :async :promise :datomic} opt-trans))
            (get-in spirit [:result :transact])
            
            :else
            (error "WRAP_TRANSACT_RESULTS: " opt-trans)))))

(defn transact-base [spirit]
  (let [opt-trans  (-> spirit :transact)
        [datomic-fn sel-key res-key]
        (cond (-> spirit :simulate)
              [datomic/with :db :simulation]

              :else
              [(if (= opt-trans :async)
                 datomic/transact-async
                 datomic/transact)
               :connection :promise])]
    (let [datoms (-> spirit :process :emitted)
          result (datomic-fn (get spirit sel-key) datoms)]
      (assoc-in spirit [:result res-key] result))))

(defn prepare-tempids [spirit]
  (let [input (-> spirit :process :input)
        review (walk/postwalk
                (fn [form]
                  (cond (db-id? form)
                        (do (swap! (:tempids spirit) conj form)
                            form)
                        (and (hash-map? form)
                             (:db/id form))
                        (let [id (:db/id form)]
                          (-> form
                              (dissoc :db/id)
                              (assoc-in [:db :id] id)))
                        :else form))
                input)]
    (update-in spirit [:process] assoc :reviewed review :emitted input)))

(def transact-fn
  (-> transact-base
      (wrap-transact-options)
      (wrap-transact-results)
      (select/wrap-pull-raw)))

(defn transact! [spirit datoms opts]
  (-> spirit
      (prepare/prepare opts datoms)
      (prepare-tempids)
      (assoc-nil :op :transact)
      transact-fn))

(defn insert! [spirit data opts]
  (let [opts (merge-nil-nested opts {:options {:schema-restrict true
                                               :schema-required true
                                               :schema-defaults true}})
        datom-fn    (-> gen-datoms
                        (wrap-datom-vector))]
    (-> spirit
        (prepare/prepare opts data)
        (assoc-nil :op :insert)
        datom-fn
        transact-fn)))

(defn wrap-delete-results
  ([f] (wrap-delete-results f :db-before))
  ([f db-state]
     (fn [spirit]
        (let [spirit (f spirit)
              opt-trans (-> spirit :transact)
              result  (-> spirit :result :transact)]
          (if (or (nil? opt-trans)
                  (= :resolve opt-trans))
            (let [ids (set (map second result))
                  struct  (or (-> spirit :result :simulation)
                              @(-> spirit :result :promise))]
              (select/select (assoc spirit :db (db-state struct))
                             ids {:options {:spirit false
                                            :ban-ids false
                                            :ban-top-id false
                                            :ids true}}))
            result)))))

(defn delete! [spirit data opts]
  (let [ids (select/select spirit data
                           (merge-nested opts {:options {:first false
                                                         :raw false}
                                               :return :ids}))
        transact-fn   (-> transact-base
                          (wrap-transact-options)
                          (wrap-delete-results)
                          (select/wrap-pull-raw))
        datoms (map (fn [x] [:db.fn/retractEntity x]) ids)]
    (-> spirit
        (prepare/prepare opts datoms)
        (prepare-tempids)
        (assoc :op :delete)
        transact-fn)))


(defn update! [spirit data update opts]
  (let [ids (select/select spirit data
                           (merge-nested opts {:options {:first false
                                                         :raw false}
                                               :return :ids}))
        updates (mapv (fn [id] (assoc-in update [:db :id] id)) ids)
        spirit (if (-> spirit :options :ban-ids)
              (-> spirit
                  (assoc-in [:options :ban-ids] false)
                  (assoc-in [:options :ban-body-ids] true))
              spirit)]
    (-> spirit
        (assoc :op :modify)
        (insert! updates (merge-nil-nested
                          opts {:options {:schema-required false
                                          :schema-defaults false}})))))

(defn delete-all! [spirit data opts]
  (let [sspirit (select/select spirit data
                            (-> opts
                                (merge-nested {:options {:first false
                                                         :raw false
                                                         :spirit true}
                                               :return :entities})
                                (update-in [:pipeline] dissoc :pull)))
        entities (-> sspirit :result :entities)
        ret-model (if-let [imodel (-> sspirit :pipeline :allow)]
                    (model/model-unpack imodel (-> sspirit :schema :tree))
                    (raise :missing-allow-model))
        all-ids (mapcat (fn [entity]
                          (link/linked-ids entity ret-model
                                           (-> sspirit :schema :flat)))
                        entities)
        transact-fn   (-> transact-base
                          (wrap-transact-options)
                          (wrap-delete-results)
                          (select/wrap-pull-raw))
        datoms (map (fn [x] [:db.fn/retractEntity x]) all-ids)
        result (-> spirit
                   (prepare/prepare (merge opts {:transact :datomic}) datoms)
                   (prepare-tempids)
                   (assoc :op :delete)
                   transact-fn)]
    (let [opt-trans (-> spirit :transact)]
      (cond (or (nil? opt-trans)
                (= :resolve opt-trans))
            (select/select (assoc spirit :db (:db-before result))
                           (set (map :db/id entities))
                           (merge opts {:options {:spirit false
                                                  :ban-ids false
                                                  :ban-top-id false
                                                  :ids true}}))

            :else
            (= :datomic opt-trans) result

            :else ("DELETE-ALL!: Options for :transact are #{:resolve(default) :transact}")
        result))))
