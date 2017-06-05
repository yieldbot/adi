(ns spirit.datomic.core.transaction
  (:require [hara.common
             [checks :refer [hash-map? long?]]
             [error :refer [error]]]
            [hara.data
             [map :refer [assoc-nil]]
             [nested :refer [merge-nil-nested
                             merge-nested]]]
            [spirit.datomic.data :refer [iid-seed]]
            [spirit.datomic.data.checks :refer [db-id?]]
            [spirit.datomic.core
             [prepare :as prepare]
             [select :as select]
             [model :as model]
             [link :as link]
             [depack :as depack]]
            [spirit.datomic.process
             [pipeline :as pipeline]
             [pack :as pack]
             [unpack :as unpack]
             [emit :as emit]]
            [hara.event :refer [raise]]
            [datomic.api :as datomic]
            [clojure.walk :as walk]))

(defn gen-datoms [datasource]
  (-> datasource
      (assoc :command :datoms)
      (pipeline/normalise)
      (pack/pack)
      (emit/emit)))

(defn wrap-datom-vector [f]
  (fn [datasource]
    (let [data (-> datasource :process :input)]
      (if (vector? data)
        ((select/wrap-merge-results f mapcat) datasource)
        (f datasource)))))

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

(defn resolve-tempids [datasource result]
  (let [ids (->> (-> datasource :tempids deref)
                 (map #(vector (iid-seed %)
                               (datomic/resolve-tempid (datomic/db (:connection datasource))
                                                       (:tempids result)
                                                       %)))
                 (into {}))]
    (depack/depack (replace-ids (-> datasource :process :reviewed) ids)
                   datasource)))

(defn wrap-transact-options [f]
  (fn [datasource]
    (let [datasource (f datasource)
          opt-trans (-> datasource :transact)
          result (cond (#{:async :promise} opt-trans)
                       (-> datasource :result :promise)

                       :else
                       (let [transact (or (-> datasource :result :simulation)
                                          @(-> datasource :result :promise))]
                         (cond (= :datomic opt-trans) transact

                               (or (nil? opt-trans)
                                   (= :resolve opt-trans))
                               (resolve-tempids datasource transact)

                               :else
                               (error "WRAP_TRANSACT_OPTIONS: #{:datomic :async :promise :resolve(default)}"))))]
      (assoc-in datasource [:result :transact] result))))

(defn wrap-transact-results [f]
  (fn [datasource]
    (let [datasource (f datasource)
          opt-trans (-> datasource :transact)]

      (cond (-> datasource :options :debug)
            datasource

            (or (nil? opt-trans)
                (#{:resolve :async :promise :datomic} opt-trans))
            (get-in datasource [:result :transact])
            
            :else
            (error "WRAP_TRANSACT_RESULTS: " opt-trans)))))

(defn transact-base [datasource]
  (let [opt-trans  (-> datasource :transact)
        [datomic-fn sel-key res-key]
        (cond (-> datasource :simulate)
              [datomic/with :db :simulation]

              :else
              [(if (= opt-trans :async)
                 datomic/transact-async
                 datomic/transact)
               :connection :promise])]
    (let [datoms (-> datasource :process :emitted)
          result (datomic-fn (get datasource sel-key) datoms)]
      (assoc-in datasource [:result res-key] result))))

(defn prepare-tempids [datasource]
  (let [input (-> datasource :process :input)
        review (walk/postwalk
                (fn [form]
                  (cond (db-id? form)
                        (do (swap! (:tempids datasource) conj form)
                            form)
                        (and (hash-map? form)
                             (:db/id form))
                        (let [id (:db/id form)]
                          (-> form
                              (dissoc :db/id)
                              (assoc-in [:db :id] id)))
                        :else form))
                input)]
    (update-in datasource [:process] assoc :reviewed review :emitted input)))

(def transact-fn
  (-> transact-base
      (wrap-transact-options)
      (wrap-transact-results)
      (select/wrap-pull-raw)))

(defn transact! [datasource datoms opts]
  (-> datasource
      (prepare/prepare opts datoms)
      (prepare-tempids)
      (assoc-nil :op :transact)
      transact-fn))

(defn insert! [datasource data opts]
  (let [opts (merge-nil-nested opts {:options {:schema-restrict true
                                               :schema-required true
                                               :schema-defaults true}})
        datom-fn    (-> gen-datoms
                        (wrap-datom-vector))]
    (-> datasource
        (prepare/prepare opts data)
        (assoc-nil :op :insert)
        datom-fn
        transact-fn)))

(defn wrap-delete-results
  ([f] (wrap-delete-results f :db-before))
  ([f db-state]
     (fn [datasource]
        (let [datasource (f datasource)
              opt-trans (-> datasource :transact)
              result  (-> datasource :result :transact)]
          (if (or (nil? opt-trans)
                  (= :resolve opt-trans))
            (let [ids (set (map second result))
                  struct  (or (-> datasource :result :simulation)
                              @(-> datasource :result :promise))]
              (select/select (assoc datasource :db (db-state struct))
                             ids {:options {:debug false
                                            :ban-ids false
                                            :ban-top-id false
                                            :ids true}}))
            result)))))

(defn delete! [datasource data opts]
  (let [ids (select/select datasource data
                           (merge-nested opts {:options {:first false
                                                         :raw false}
                                               :return :ids}))
        transact-fn   (-> transact-base
                          (wrap-transact-options)
                          (wrap-delete-results)
                          (select/wrap-pull-raw))
        datoms (map (fn [x] [:db.fn/retractEntity x]) ids)]
    (-> datasource
        (prepare/prepare opts datoms)
        (prepare-tempids)
        (assoc :op :delete)
        transact-fn)))


(defn update! [datasource data update opts]
  (let [ids (select/select datasource data
                           (merge-nested opts {:options {:first false
                                                         :raw false}
                                               :return :ids}))
        updates (mapv (fn [id] (assoc-in update [:db :id] id)) ids)
        datasource (if (-> datasource :options :ban-ids)
              (-> datasource
                  (assoc-in [:options :ban-ids] false)
                  (assoc-in [:options :ban-body-ids] true))
              datasource)]
    (-> datasource
        (assoc :op :modify)
        (insert! updates (merge-nil-nested
                          opts {:options {:schema-required false
                                          :schema-defaults false}})))))

(defn delete-all! [datasource data opts]
  (let [sdatasource (select/select datasource data
                                   (-> opts
                                       (merge-nested {:options {:first false
                                                                :raw false
                                                                :debug true}
                                                      :return :entities})
                                       (update-in [:pipeline] dissoc :pull)))
        entities (-> sdatasource :result :entities)
        ret-model (if-let [imodel (-> sdatasource :pipeline :allow)]
                    (model/model-unpack imodel (-> sdatasource :schema :tree))
                    (raise :missing-allow-model))
        all-ids (mapcat (fn [entity]
                          (link/linked-ids entity ret-model
                                           (-> sdatasource :schema :flat)))
                        entities)
        transact-fn   (-> transact-base
                          (wrap-transact-options)
                          (wrap-delete-results)
                          (select/wrap-pull-raw))
        datoms (map (fn [x] [:db.fn/retractEntity x]) all-ids)
        result (-> datasource
                   (prepare/prepare (merge opts {:transact :datomic}) datoms)
                   (prepare-tempids)
                   (assoc :op :delete)
                   transact-fn)]
    (let [opt-trans (-> datasource :transact)]
      (cond (or (nil? opt-trans)
                (= :resolve opt-trans))
            (select/select (assoc datasource :db (:db-before result))
                           (set (map :db/id entities))
                           (merge opts {:options {:debug false
                                                  :ban-ids false
                                                  :ban-top-id false
                                                  :ids true}}))

            :else
            (= :datomic opt-trans) result

            :else ("DELETE-ALL!: Options for :transact are #{:resolve(default) :transact}")
        result))))
