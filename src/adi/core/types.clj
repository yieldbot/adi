(ns adi.core.types
  (:require [datomic.api :as datomic]))

(defn last-transaction-time [db]
  (let [t (-> db datomic/basis-t)]
    [t (ffirst (datomic/q '[:find ?t
                            :in $ ?tx
                            :where [?tx :db/txInstant ?t]]
                          db
                          (datomic/t->tx t)))]))

(defmethod print-method datomic.peer.LocalConnection
  [conn w]
  (.write w  (if-let [db (try (datomic/db conn)
                              (catch Exception t))]
               (let [[t dt] (last-transaction-time db)]
                 (format "#connection%s" {t dt}))
               "#connection{}")))

(defmethod print-method datomic.db.Db
  [db w]
  (.write w (if-let [[t dt] (last-transaction-time db)]
              (format "#db%s" {t dt})
              "#db{}")))

(defrecord Adi [meta]
  Object
  (toString [this]
    (str "#adi" (into {} (dissoc this :meta)))))

(defmethod print-method Adi
  [v w]
  (.write w (str v)))
