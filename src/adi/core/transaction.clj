(ns adi.core.transaction
  (:require [hara.common.checks :refer [hash-map? long?]]
            [adi.core
             [environment :as environment]
             [select :as select]]
            [adi.process
             [normalise :as normalise]
             [pack :as pack]
             [emit :as emit]
             [unpack :as unpack]]
            [ribol.core :refer [raise]]
            [datomic.api :as datomic]))

(defn gen-datoms [adi data]
  (let [adi (-> adi
                (assoc :type "datoms"))]
    (-> data
        (normalise/normalise adi)
        (pack/pack adi)
        (emit/emit adi))))

(defn wrap-transaction-return [f & [ids]]
  (fn [adi data]
    (let [trs  (:transact adi)
          dtms (f adi data)]
      (cond (-> adi :options :raw)
            dtms

            (-> adi :options :simulate)
            (assoc adi :db (:db-after (datomic/with (:db adi) dtms)))

            :else
            (condp = trs
              nil      @(datomic/transact (:connection adi) dtms)
              :promise (datomic/transact (:connection adi) dtms)
              :async   (datomic/transact-async (:connection adi) dtms)
              :full    (let [res @(datomic/transact (:connection adi) dtms)
                             ndb (:db-after res)]
                         (if ids
                           (select/select-fn (assoc adi :db ndb) ids :select
                                             #(unpack/unpack (datomic/entity ndb %) adi))
                           res))
              :compare (let [res @(datomic/transact (:connection adi) dtms)
                             ndb (:db-after res)]
                         (if ids
                           [(select/select-fn adi ids :select
                                              #(unpack/unpack (datomic/entity (:db adi) %) adi))
                            (select/select-fn (assoc adi :db ndb) ids :select
                                              #(unpack/unpack (datomic/entity ndb %) adi))]
                           res)))))))

(defn wrap-vector-inserts [f]
  (fn [adi data]
    (if (vector? data)
      (let [vfn (fn [adi data]
                  (mapcat #(f adi %) data))]
        (vfn adi data))
      (f adi data))))

(defn insert! [adi data & args]
  (let [adi (environment/setup adi args)
        in-fn (-> gen-datoms
                  (wrap-vector-inserts)
                  (wrap-transaction-return))]
    (in-fn (assoc adi :op :insert) data)))

(defn transact! [adi data & args]
  (let [adi (environment/setup adi args)
        in-fn (-> (fn [adi data] data)
                  (wrap-transaction-return))]
    (in-fn (assoc adi :op :transact) data)))

(defn delete! [adi data & args]
  (let [adi (environment/setup adi args)
        ids (select/select-fn (assoc-in adi [:options :raw] false)
                              data :delete identity)
        del-fn (-> (fn [adi data]
                     (map (fn [x] [:db.fn/retractEntity x]) ids))
                   (wrap-transaction-return ids))]
    (del-fn adi data)))

(defn update! [adi data update & args]
  (let [adi (environment/setup adi args)
        ids (select/select-fn (assoc-in adi [:options :raw] false)
                              data :update identity)
        updates (mapv (fn [id] (assoc update :db/id id)) ids)
        adi (if (-> adi :options :ban-ids)
              (-> adi
                  (assoc-in [:options :ban-ids] false)
                  (assoc-in [:options :ban-body-ids] true))
              adi)
        in-fn (-> gen-datoms
                  (wrap-vector-inserts)
                  (wrap-transaction-return ids))]
    (in-fn (assoc adi :op :update) updates)))
