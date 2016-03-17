(ns adi.core.transaction
  (:require [hara.common
             [checks :refer [hash-map? long?]]
             [error :refer [error]]]
            [hara.data
             [map :refer [assoc-nil]]
             [nested :refer [merge-nil-nested
                             merge-nested]]]
            [adi.data
             [common :refer [iid-seed]]
             [checks :refer [db-id?]]]
            [adi.core
             [prepare :as prepare]
             [select :as select]
             [model :as model]
             [link :as link]
             [depack :as depack]]
            [adi.process
             [normalise :as normalise]
             [pack :as pack]
             [unpack :as unpack]
             [emit :as emit]]
            [hara.event :refer [raise]]
            [datomic.api :as datomic]
            [clojure.walk :as walk]))

(defn gen-datoms [adi]
  (-> adi
      (assoc :type "datoms")
      (normalise/normalise)
      (pack/pack)
      (emit/emit)))

(defn wrap-datom-vector [f]
  (fn [adi]
    (let [data (-> adi :process :input)]
      (if (vector? data)
        ((select/wrap-merge-results f mapcat) adi)
        (f adi)))))

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

(defn resolve-tempids [adi result]
  (let [ids (->> (-> adi :tempids deref)
                 (map #(vector (iid-seed %)
                               (datomic/resolve-tempid (datomic/db (:connection adi))
                                                       (:tempids result)
                                                       %)))
                 (into {}))]
    (depack/depack (replace-ids (-> adi :process :reviewed) ids)
                   adi)))

(defn wrap-transact-options [f]
  (fn [adi]
    (let [adi (f adi)
          opt-trans (-> adi :transact)
          result (cond (#{:async :promise} opt-trans)
                       (-> adi :result :promise)

                       :else
                       (let [transact (or (-> adi :result :simulation)
                                          @(-> adi :result :promise))]
                         (cond (= :datomic opt-trans) transact

                               (or (nil? opt-trans)
                                   (= :resolve opt-trans))
                               (resolve-tempids adi transact)

                               :else
                               (error "WRAP_TRANSACT_OPTIONS: #{:datomic :async :promise :resolve(default)}"))))]
      (assoc-in adi [:result :transact] result))))

(defn wrap-transact-results [f]
  (fn [adi]
    (let [adi (f adi)
          opt-trans (-> adi :transact)]

      (cond (-> adi :options :adi)
            adi

            (or (nil? opt-trans)
                (#{:resolve :async :promise :datomic} opt-trans))
            (get-in adi [:result :transact])
            
            :else
            (error "WRAP_TRANSACT_RESULTS: " opt-trans)))))

(defn transact-base [adi]
  (let [opt-trans  (-> adi :transact)
        [datomic-fn sel-key res-key]
        (cond (-> adi :options :simulate)
              [datomic/with :db :simulation]

              :else
              [(if (= opt-trans :async)
                 datomic/transact-async
                 datomic/transact)
               :connection :promise])]
    (let [datoms (-> adi :process :emitted)
          result (datomic-fn (get adi sel-key) datoms)]
      (assoc-in adi [:result res-key] result))))

(defn prepare-tempids [adi]
  (let [input (-> adi :process :input)
        review (walk/postwalk
                (fn [form]
                  (cond (db-id? form)
                        (do (swap! (:tempids adi) conj form)
                            form)
                        (and (hash-map? form)
                             (:db/id form))
                        (let [id (:db/id form)]
                          (-> form
                              (dissoc :db/id)
                              (assoc-in [:db :id] id)))
                        :else form))
                input)]
    (update-in adi [:process] assoc :reviewed review :emitted input)))

(def transact-fn
  (-> transact-base
      (wrap-transact-options)
      (wrap-transact-results)
      (select/wrap-pull-raw)))

(defn transact! [adi datoms opts]
  (-> adi
      (prepare/prepare opts datoms)
      (prepare-tempids)
      (assoc-nil :op :transact)
      transact-fn))

(defn insert! [adi data opts]
  (let [opts (merge-nil-nested opts {:options {:schema-restrict true
                                               :schema-required true
                                               :schema-defaults true}})
        datom-fn    (-> gen-datoms
                        (wrap-datom-vector))]
    (-> adi
        (prepare/prepare opts data)
        (assoc-nil :op :insert)
        datom-fn
        transact-fn)))

(defn wrap-delete-results
  ([f] (wrap-delete-results f :db-before))
  ([f db-state]
     (fn [adi]
        (let [adi (f adi)
              opt-trans (-> adi :transact)
              result  (-> adi :result :transact)]
          (if (or (nil? opt-trans)
                  (= :resolve opt-trans))
            (let [ids (set (map second result))
                  struct  (or (-> adi :result :simulation)
                              @(-> adi :result :promise))]
              (select/select (assoc adi :db (db-state struct))
                             ids {:options {:adi false
                                            :ban-ids false
                                            :ban-top-id false
                                            :ids true}}))
            result)))))

(defn delete! [adi data opts]
  (let [ids (select/select adi data
                           (merge-nested opts {:options {:first false
                                                         :raw false}
                                               :return :ids}))
        transact-fn   (-> transact-base
                          (wrap-transact-options)
                          (wrap-delete-results)
                          (select/wrap-pull-raw))
        datoms (map (fn [x] [:db.fn/retractEntity x]) ids)]
    (-> adi
        (prepare/prepare opts datoms)
        (prepare-tempids)
        (assoc :op :delete)
        transact-fn)))


(defn update! [adi data update opts]
  (let [ids (select/select adi data
                           (merge-nested opts {:options {:first false
                                                         :raw false}
                                               :return :ids}))
        updates (mapv (fn [id] (assoc-in update [:db :id] id)) ids)
        adi (if (-> adi :options :ban-ids)
              (-> adi
                  (assoc-in [:options :ban-ids] false)
                  (assoc-in [:options :ban-body-ids] true))
              adi)]
    (-> adi
        (assoc :op :modify)
        (insert! updates (merge-nil-nested
                          opts {:options {:schema-required false
                                          :schema-defaults false}})))))

(defn delete-all! [adi data opts]
  (let [sadi (select/select adi data
                            (-> opts
                                (merge-nested {:options {:first false
                                                         :raw false
                                                         :adi true}
                                               :return :entities})
                                (update-in [:pipeline] dissoc :pull)))
        entities (-> sadi :result :entities)
        ret-model (if-let [imodel (-> sadi :pipeline :allow)]
                 (model/model-unpack imodel (-> sadi :schema :tree))
                 (raise :missing-allow-model))
        all-ids (mapcat (fn [entity]
                          (link/linked-ids entity ret-model
                                           (-> sadi :schema :flat)))
                        entities)
        transact-fn   (-> transact-base
                          (wrap-transact-options)
                          (wrap-delete-results)
                          (select/wrap-pull-raw))
        datoms (map (fn [x] [:db.fn/retractEntity x]) all-ids)
        result (-> adi
                   (prepare/prepare (merge opts {:transact :datomic}) datoms)
                   (prepare-tempids)
                   (assoc :op :delete)
                   transact-fn)]
    (let [opt-trans (-> adi :transact)]
      (cond (or (nil? opt-trans)
                (= :resolve opt-trans))
            (select/select (assoc adi :db (:db-before result))
                           (set (map :db/id entities))
                           (merge opts {:options {:adi false
                                                  :ban-ids false
                                                  :ban-top-id false
                                                  :ids true}}))

            :else
            (= :datomic opt-trans) result

            :else ("DELETE-ALL!: Options for :transact are #{:resolve(default) :transact}")
        result))))
