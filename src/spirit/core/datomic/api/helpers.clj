(ns spirit.core.datomic.api.helpers
  (:require [datomic.api :as datomic]))

(defn transactions
  ([datasource attr]
     (->> (datomic/q '[:find ?tx
                       :in $ ?a
                       :where [_ ?a ?v ?tx _]
                       [?tx :db/txInstant ?tx-time]]
                     (datomic/history (datomic/db (:connection datasource)))
                     attr)
          (map #(first %))
          (map datomic/tx->t)
          (sort)))
  ([datasource attr val]
     (->> (datomic/q '[:find ?tx
                       :in $ ?a ?v
                       :where [_ ?a ?v ?tx _]
                       [?tx :db/txInstant ?tx-time]]
                     (datomic/history (datomic/db (:connection datasource)))
                     attr val)
          (map #(first %))
          (map datomic/tx->t)
          (sort))))
          
(defn transaction-time [datasource t]
  (->> (datomic/q '[:find ?t
                    :in $ ?tx
                    :where [?tx :db/txInstant ?t]]
                  (datomic/db (:connection datasource))
                  (datomic/t->tx t))
        (ffirst)))

(defn schema-properties [datasource]
  (let [data (datomic/q '[:find ?ident ?type ?cardinality ?e :where
                          [?e :db/ident ?ident]
                          [?e :db/valueType ?t]
                          [?t :db/ident ?type]
                          [?e :db/cardinality ?c]
                          [?c :db/ident ?cardinality]]
                        (datomic/db (:connection datasource)))]
    (zipmap (map first data) data)))
