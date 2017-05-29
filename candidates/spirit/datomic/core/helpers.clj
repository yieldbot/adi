(ns spirit.core.helpers
  (:require [datomic.api :as datomic]))

(defn transactions
  ([spirit attr]
     (->> (datomic/q '[:find ?tx
                       :in $ ?a
                       :where [_ ?a ?v ?tx _]
                       [?tx :db/txInstant ?tx-time]]
                     (datomic/history (datomic/db (:connection spirit)))
                     attr)
          (map #(first %))
          (map datomic/tx->t)
          (sort)))
  ([spirit attr val]
     (->> (datomic/q '[:find ?tx
                       :in $ ?a ?v
                       :where [_ ?a ?v ?tx _]
                       [?tx :db/txInstant ?tx-time]]
                     (datomic/history (datomic/db (:connection spirit)))
                     attr val)
          (map #(first %))
          (map datomic/tx->t)
          (sort))))
          
(defn transaction-time [spirit t]
  (->> (datomic/q '[:find ?t
                    :in $ ?tx
                    :where [?tx :db/txInstant ?t]]
                  (datomic/db (:connection spirit))
                  (datomic/t->tx t))
        (ffirst)))

(defn schema-properties [spirit]
  (let [data (datomic/q '[:find ?ident ?type ?cardinality ?e :where
                          [?e :db/ident ?ident]
                          [?e :db/valueType ?t]
                          [?t :db/ident ?type]
                          [?e :db/cardinality ?c]
                          [?c :db/ident ?cardinality]]
                        (datomic/db (:connection spirit)))]
    (zipmap (map first data) data)))
