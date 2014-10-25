(ns adi.core.select
  (:require [hara.common.checks :refer [hash-map? long?]]
            [adi.core.environment :as environment]
            [adi.process
             [normalise :as normalise]
             [pack :as pack]
             [emit :as emit]
             [unpack :as unpack]]
            [ribol.core :refer [raise]]
            [datomic.api :as datomic]))

(defn wrap-select-data [f]
  (fn [adi data]
    (cond (long? data)
          (if (or (-> adi :options :ban-ids)
                  (-> adi :options :ban-top-id))
            (raise [:id-banned {:data data}])
            #{data})

          (hash-map? data)
          (let [id  (or (get data :db/id)
                        (get-in data [:db :id]))
                qry (f adi data)]
            (if (-> adi :options :raw)
              #{qry}
              (let [res (->> (datomic/q qry (:db adi))
                             (map first)
                             (set))]
                (if id
                  (if (get res id) #{id} #{})
                  res)))))))

(defn wrap-select-return [f ret-fn]
  (fn [adi data]
    (let [res (f adi data)]
      (cond (-> adi :options :raw)
            res

            (-> adi :options :first)
            (if-let [fst (first res)]
              (ret-fn fst))

            :else
            (set (map ret-fn res))))))

(defn wrap-select-keyword [f]
  (fn [adi data]
    (if (keyword? data)
      (if (-> adi :options :ban-underscores)
        (raise [:keyword-banned {:data data}])
        (f adi {data '_}))
      (f adi data))))

(defn wrap-select-set [f]
  (fn [adi data]
    (if (set? data)
      (set (mapcat #(f adi %) data))
      (f adi data))))

(defn gen-query [adi data]
  (let [adi (-> adi
                (update-in [:options]
                           dissoc
                           :schema-required
                           :schema-restrict
                           :schema-defaults)
                (assoc :type "query"))]
    (-> data
        (normalise/normalise adi)
        (pack/pack adi)
        (emit/emit adi))))

(defn select-fn [adi data op ret-fn]
  (let [sel-fn (-> gen-query
                   (wrap-select-data)
                   (wrap-select-keyword)
                   (wrap-select-set)
                   (wrap-select-return ret-fn))]
    (sel-fn (assoc adi :op op) data)))

(defn select-ids [adi data & args]
  (let [adi (environment/setup adi args)]
    (select-fn adi data :select identity)))

(defn select-entities [adi data & args]
  (let [adi (environment/setup adi args)]
    (select-fn adi data :select #(datomic/entity (:db adi) %))))

(defn select [adi data & args]
  (let [adi (environment/setup adi args)
        view-fn #(unpack/unpack (datomic/entity (:db adi) %) adi)]
    (select-fn adi data :select view-fn)))
