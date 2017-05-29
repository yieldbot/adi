(ns spirit.core.select
  (:require [hara.common
             [checks :refer [hash-map? long?]]
             [error :refer [error]]]
            [hara.data.map :refer [assoc-nil]]
            [spirit.core
             [prepare :as prepare]
             [types :as types]]
            [spirit.process
             [normalise :as normalise]
             [pack :as pack]
             [emit :as emit]
             [unpack :as unpack]]
            [datomic.api :as datomic]
            [hara.event :refer [raise]])
  (:import spirit.core.types.Adi))

(defn wrap-merge-results
  ([f] (wrap-merge-results f mapcat))
  ([f merge-emit-fn]
     (fn [spirit]
       (let [processes (->> (-> spirit :process :input)
                             (map #(f (assoc-in spirit [:process :input] %)))
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
          (assoc spirit :process (assoc inputs :emitted emitted))))))

(defn wrap-query-keyword [f]
  (fn [spirit]
    (let [data (-> spirit :process :input)]
      (if (keyword? data)
        (if (-> spirit :options :ban-underscores)
          (raise [:keyword-banned {:data data}])
          (f (assoc-in spirit [:process :input] {data '_})))
        (f spirit)))))

(defn wrap-query-set [f]
  (fn [spirit]
    (let [data (-> spirit :process :input)]
      (if (set? data)
        ((wrap-merge-results f #(set (map %1 %2))) spirit)
        (f spirit)))))

(defn wrap-query-data [f]
  (fn [spirit]
    (let [data (-> spirit :process :input)]
      (cond (long? data)
            (if (or (-> spirit :options :ban-ids)
                    (-> spirit :options :ban-top-id))
              (raise [:id-banned {:data data}])
              (-> spirit
                  (update-in [:process]
                             #(apply assoc % (zipmap [:normalised :analysed :reviewed :characterised]
                                                     (repeat data))))
                  (assoc-in [:process :emitted] (list data))))

            (hash-map? data)
            (f spirit)

            :else
            (error "WRAP_QUERY_DATA: hash-map and long only: " data)))))

(defn gen-query [spirit]
  (-> spirit
      (update-in [:options]
                 dissoc
                 :schema-required
                 :schema-restrict
                 :schema-defaults)
      (assoc :type "query")
      (normalise/normalise)
      (pack/pack)
      (emit/emit)))

(defn wrap-pull-raw [f]
  (fn [spirit]
    (if (and (not (-> spirit :options :spirit))
             (-> spirit :options :raw))
      (-> spirit :process :emitted)
      (f spirit))))

(defn wrap-pull-first [f]
  (fn [spirit]
    (let [result (f spirit)]
      (if (instance? Adi result)
        result
        (if (-> spirit :options :first)
          (first result)
          (set result))))))

(defn wrap-pull-entities [f]
  (fn [spirit]
    (let [spirit (f spirit)]
      (if (-> spirit :return (= :ids))
        spirit
        (let [ids (-> spirit :result :ids)
              ents (map #(datomic/entity (:db spirit) %) ids)]
          (assoc-in spirit [:result :entities] ents))))))

(defn wrap-pull-data [f]
  (fn [spirit]
    (let [spirit (f spirit)]
      (if (-> spirit :return (#{:ids :entities}))
        spirit
        (let [entities (-> spirit :result :entities)
              data  (-> (map #(unpack/unpack % spirit) entities))]
          (assoc-in spirit [:result :data] data))))))

(defn wrap-pull-spirit [f]
  (fn [spirit]
    (let [spirit (f spirit)]
      (if (-> spirit :options :spirit)
        spirit
        (let [ret (or (:return spirit) :data)]
          (get-in spirit [:result ret]))))))

(defn select-base [spirit]
  (let [qry (-> spirit :process :emitted)
        pull-fn (fn [qry]
                    (if (list? qry) qry
                        (->> (datomic/q qry (:db spirit))
                             (map first))))
        results (if (set? qry)
                  (mapcat pull-fn qry)
                  (pull-fn qry))]
    (assoc-in spirit [:result :ids] results)))

(defn select [spirit data opts]
  (let [query-fn  (-> gen-query
                      (wrap-query-data)
                      (wrap-query-keyword)
                      (wrap-query-set))
        pull-fn (-> select-base
                    (wrap-pull-entities)
                    (wrap-pull-data)
                    (wrap-pull-spirit)
                    (wrap-pull-first)
                    (wrap-pull-raw))]
    (-> spirit
        (prepare/prepare opts data)
        (assoc-nil :op :select)
        query-fn
        pull-fn)))

(defn query [spirit data qargs opts]
  (let [pull-fn  (-> (fn [spirit]
                       (assoc-in spirit [:result :ids]
                                 (map first (apply datomic/q data (:db spirit) qargs))))
                     (wrap-pull-entities)
                     (wrap-pull-data)
                     (wrap-pull-spirit)
                     (wrap-pull-first)
                     (wrap-pull-raw))]
    (-> spirit
        (prepare/prepare opts data)
        (assoc :op :query)
        pull-fn)))
