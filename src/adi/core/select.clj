(ns adi.core.select
  (:require [hara.common
             [checks :refer [hash-map? long?]]
             [error :refer [error]]]
            [hara.data.map :refer [assoc-nil]]
            [adi.core
             [prepare :as prepare]
             [types :as types]]
            [adi.process
             [normalise :as normalise]
             [pack :as pack]
             [emit :as emit]
             [unpack :as unpack]]
            [datomic.api :as datomic]
            [ribol.core :refer [raise]])
  (:import adi.core.types.Adi))

(defn wrap-merge-results
  ([f] (wrap-merge-results f mapcat))
  ([f merge-emit-fn]
     (fn [adi]
       (let [processes (->> (-> adi :process :input)
                             (map #(f (assoc-in adi [:process :input] %)))
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
          (assoc adi :process (assoc inputs :emitted emitted))))))

(defn wrap-query-keyword [f]
  (fn [adi]
    (let [data (-> adi :process :input)]
      (if (keyword? data)
        (if (-> adi :options :ban-underscores)
          (raise [:keyword-banned {:data data}])
          (f (assoc-in adi [:process :input] {data '_})))
        (f adi)))))

(defn wrap-query-set [f]
  (fn [adi]
    (let [data (-> adi :process :input)]
      (if (set? data)
        ((wrap-merge-results f #(set (map %1 %2))) adi)
        (f adi)))))

(defn wrap-query-data [f]
  (fn [adi]
    (let [data (-> adi :process :input)]
      (cond (long? data)
            (if (or (-> adi :options :ban-ids)
                    (-> adi :options :ban-top-id))
              (raise [:id-banned {:data data}])
              (-> adi
                  (update-in [:process]
                             #(apply assoc % (zipmap [:normalised :analysed :reviewed :characterised]
                                                     (repeat data))))
                  (assoc-in [:process :emitted] (list data))))

            (hash-map? data)
            (f adi)

            :else
            (error "WRAP_QUERY_DATA: hash-map and long only: " data)))))

(defn gen-query [adi]
  (-> adi
      (update-in [:options]
                 dissoc
                 :schema-required
                 :schema-restrict
                 :schema-defaults)
      (assoc :type "query")
      (normalise/normalise)
      (pack/pack)
      (emit/emit)))

(defn wrap-return-raw [f]
  (fn [adi]
    (if (-> adi :options :raw)
      (-> adi :process :emitted)
      (f adi))))

(defn wrap-return-first [f]
  (fn [adi]
    (let [result (f adi)]
      (if (instance? Adi result)
        result
        (if (-> adi :options :first)
          (first result)
          (set result))))))

(defn wrap-return-ids [f]
  (fn [adi]
    (let [adi (f adi)]
      (if (-> adi :get (= :ids))
        (-> adi :result :ids)
        adi))))

(defn wrap-return-entities [f]
  (fn [adi]
    (let [result (f adi)]
      (if (instance? Adi result)
        (let [ids (-> result :result :ids)
              ents (map #(datomic/entity (:db adi) %) ids)]
          (if (-> result :get (= :entities))
            ents
            (assoc-in result [:result :entities] ents)))
        result))))

(defn wrap-return-data [f]
  (fn [adi]
    (let [result (f adi)]
      (if (instance? Adi result)
        (let [entities (-> result :result :entities)
              data  (-> (map #(unpack/unpack % adi) entities))]
          (if (-> result :get (#(or (nil? %)
                                    (= :data %))))
            data
            (assoc-in result [:result :data] data)))
        result))))

(defn wrap-return-adi [f]
  (fn [adi]
    (let [result (f adi)]
      (if (instance? Adi result)
        (if (-> result :get (= :all))
          result
          (error "Options for get are #{:data(default), :ids, :entities, :all}, not " (:get result)))
        result))))

(defn select-base [adi]
  (let [qry (-> adi :process :emitted)
        return-fn (fn [qry]
                    (if (list? qry) qry
                        (->> (datomic/q qry (:db adi))
                             (map first))))
        results (if (set? qry)
                  (mapcat return-fn qry)
                  (return-fn qry))]
    (assoc-in adi [:result :ids] results)))

(defn select [adi data opts]
  (let [query-fn  (-> gen-query
                      (wrap-query-data)
                      (wrap-query-keyword)
                      (wrap-query-set))
        return-fn (-> select-base
                      (wrap-return-ids)
                      (wrap-return-entities)
                      (wrap-return-data)
                      (wrap-return-adi)
                      (wrap-return-first)
                      (wrap-return-raw))]
    (-> adi
        (prepare/prepare opts data)
        (assoc-nil :op :select)
        query-fn
        return-fn)))

(defn query [adi data qargs opts]
  (let [return-fn  (-> (fn [adi]
                         (assoc-in adi [:result :ids]
                                   (map first (apply datomic/q data (:db adi) qargs))))
                       (wrap-return-ids)
                       (wrap-return-entities)
                       (wrap-return-data)
                       (wrap-return-adi)
                       (wrap-return-first)
                       (wrap-return-raw))]
    (-> adi
        (prepare/prepare opts data)
        (assoc :op :query)
        return-fn)))
