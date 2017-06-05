(ns spirit.datomic.core.select
  (:require [hara.common
             [checks :refer [hash-map? long?]]
             [error :refer [error]]]
            [hara.data.map :refer [assoc-nil]]
            [spirit.datomic.types :as types]
            [spirit.datomic.core
             [prepare :as prepare]]
            [spirit.datomic.process
             [pipeline :as pipeline]
             [pack :as pack]
             [emit :as emit]
             [unpack :as unpack]]
            [datomic.api :as datomic]
            [hara.event :refer [raise]])
  (:import spirit.datomic.types.Datomic))

(defn wrap-merge-results
  ([f] (wrap-merge-results f mapcat))
  ([f merge-emit-fn]
     (fn [datasource]
       (let [processes (->> (-> datasource :process :input)
                             (map #(f (assoc-in datasource [:process :input] %)))
                             (map :process))
              inputs   (->> processes
                            (map #(dissoc % :emitted))
                            (apply merge-with
                                   conj
                                   {:input []
                                    :normalised []
                                    :analysed []
                                    :reviewed []
                                    :characterised []}))
              emitted  (merge-emit-fn :emitted processes)]
          (assoc datasource :process (assoc inputs :emitted emitted))))))

(defn wrap-query-keyword [f]
  (fn [datasource]
    (let [data (-> datasource :process :input)]
      (if (keyword? data)
        (if (-> datasource :options :ban-underscores)
          (raise [:keyword-banned {:data data}])
          (f (assoc-in datasource [:process :input] {data '_})))
        (f datasource)))))

(defn wrap-query-set [f]
  (fn [datasource]
    (let [data (-> datasource :process :input)]
      (if (set? data)
        ((wrap-merge-results f #(set (map %1 %2))) datasource)
        (f datasource)))))

(defn wrap-query-data [f]
  (fn [datasource]
    (let [data (-> datasource :process :input)]
      (cond (long? data)
            (if (or (-> datasource :options :ban-ids)
                    (-> datasource :options :ban-top-id))
              (raise [:id-banned {:data data}])
              (-> datasource
                  (update-in [:process]
                             #(apply assoc % (zipmap [:normalised :analysed :reviewed :characterised]
                                                     (repeat data))))
                  (assoc-in [:process :emitted] (list data))))

            (hash-map? data)
            (f datasource)

            :else
            (error "WRAP_QUERY_DATA: hash-map and long only: " data)))))

(defn gen-query [datasource]
  (-> datasource
      (update-in [:options]
                 dissoc
                 :schema-required
                 :schema-restrict
                 :schema-defaults)
      (assoc :command :query)
      (pipeline/normalise)
      (pack/pack)
      (emit/emit)))

(defn wrap-pull-raw [f]
  (fn [datasource]
    (if (and (not (-> datasource :options :debug))
             (-> datasource :options :raw))
      (-> datasource :process :emitted)
      (f datasource))))

(defn wrap-pull-first [f]
  (fn [datasource]
    (let [result (f datasource)]
      (if (instance? Datomic result)
        result
        (if (-> datasource :options :first)
          (first result)
          (set result))))))

(defn wrap-pull-entities [f]
  (fn [datasource]
    (let [datasource (f datasource)]
      (if (-> datasource :return (= :ids))
        datasource
        (let [ids (-> datasource :result :ids)
              ents (map #(datomic/entity (:db datasource) %) ids)]
          (assoc-in datasource [:result :entities] ents))))))

(defn wrap-pull-data [f]
  (fn [datasource]
    (let [datasource (f datasource)]
      (if (-> datasource :return (#{:ids :entities}))
        datasource
        (let [entities (-> datasource :result :entities)
              data  (-> (map #(unpack/unpack % datasource) entities))]
          (assoc-in datasource [:result :data] data))))))

(defn wrap-pull-datasource [f]
  (fn [datasource]
    (let [datasource (f datasource)]
      (if (-> datasource :options :debug)
        datasource
        (let [ret (or (:return datasource) :data)]
          (get-in datasource [:result ret]))))))

(defn select-base [datasource]
  (let [qry (-> datasource :process :emitted)
        pull-fn (fn [qry]
                    (if (list? qry) qry
                        (->> (datomic/q qry (:db datasource))
                             (map first))))
        results (if (set? qry)
                  (mapcat pull-fn qry)
                  (pull-fn qry))]
    (assoc-in datasource [:result :ids] results)))

(defn select [datasource data opts]
  (let [query-fn  (-> gen-query
                      (wrap-query-data)
                      (wrap-query-keyword)
                      (wrap-query-set))
        pull-fn (-> select-base
                    (wrap-pull-entities)
                    (wrap-pull-data)
                    (wrap-pull-datasource)
                    (wrap-pull-first)
                    (wrap-pull-raw))]
    (-> datasource
        (prepare/prepare opts data)
        (assoc-nil :op :select)
        query-fn
        pull-fn)))

(defn query [datasource data qargs opts]
  (let [pull-fn  (-> (fn [datasource]
                       (assoc-in datasource [:result :ids]
                                 (map first (apply datomic/q data (:db datasource) qargs))))
                     (wrap-pull-entities)
                     (wrap-pull-data)
                     (wrap-pull-datasource)
                     (wrap-pull-first)
                     (wrap-pull-raw))]
    (-> datasource
        (prepare/prepare opts data)
        (assoc :op :query)
        pull-fn)))
