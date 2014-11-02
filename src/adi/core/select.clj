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

(defn wrap-pull-raw [f]
  (fn [adi]
    (if (and (not (-> adi :options :adi))
             (-> adi :options :raw))
      (-> adi :process :emitted)
      (f adi))))

(defn wrap-pull-first [f]
  (fn [adi]
    (let [result (f adi)]
      (if (instance? Adi result)
        result
        (if (-> adi :options :first)
          (first result)
          (set result))))))

(defn wrap-pull-entities [f]
  (fn [adi]
    (let [adi (f adi)]
      (if (-> adi :return (= :ids))
        adi
        (let [ids (-> adi :result :ids)
              ents (map #(datomic/entity (:db adi) %) ids)]
          (assoc-in adi [:result :entities] ents))))))

(defn wrap-pull-data [f]
  (fn [adi]
    (let [adi (f adi)]
      (if (-> adi :return (#{:ids :entities}))
        adi
        (let [entities (-> adi :result :entities)
              data  (-> (map #(unpack/unpack % adi) entities))]
          (assoc-in adi [:result :data] data))))))

(defn wrap-pull-adi [f]
  (fn [adi]
    (let [adi (f adi)]
      (if (-> adi :options :adi)
        adi
        (let [ret (or (:return adi) :data)]
          (get-in adi [:result ret]))))))

(defn select-base [adi]
  (let [qry (-> adi :process :emitted)
        pull-fn (fn [qry]
                    (if (list? qry) qry
                        (->> (datomic/q qry (:db adi))
                             (map first))))
        results (if (set? qry)
                  (mapcat pull-fn qry)
                  (pull-fn qry))]
    (assoc-in adi [:result :ids] results)))

(defn select [adi data opts]
  (let [query-fn  (-> gen-query
                      (wrap-query-data)
                      (wrap-query-keyword)
                      (wrap-query-set))
        pull-fn (-> select-base
                      (wrap-pull-entities)
                      (wrap-pull-data)
                      (wrap-pull-adi)
                      (wrap-pull-first)
                      (wrap-pull-raw))]
    (-> adi
        (prepare/prepare opts data)
        (assoc-nil :op :select)
        query-fn
        pull-fn)))

(defn query [adi data qargs opts]
  (let [pull-fn  (-> (fn [adi]
                         (assoc-in adi [:result :ids]
                                   (map first (apply datomic/q data (:db adi) qargs))))
                       (wrap-pull-entities)
                       (wrap-pull-data)
                       (wrap-pull-adi)
                       (wrap-pull-first)
                       (wrap-pull-raw))]
    (-> adi
        (prepare/prepare opts data)
        (assoc :op :query)
        pull-fn)))
