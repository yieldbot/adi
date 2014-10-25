(ns adi.core.types
  (:require [datomic.api :as datomic]))

(defn last-transaction-time [conn]
  (if conn
    (if-let [t (try (-> conn datomic/db datomic/basis-t datomic/t->tx)
                    (catch Exception t))]
      (java.util.Date. t)) ))

(defmethod print-method datomic.peer.LocalConnection
  [v w]
  (.write w (format "#conn[\"%s\"]"
                    (if-let [dt (last-transaction-time v)]
                      (.format (java.text.SimpleDateFormat. "yyyy/MM/dd HH:mm:ssZZ" (java.util.Locale/getDefault))
                               dt)
                      "NA"))))

(defrecord Adi [meta]
  Object
  (toString [this]
    (str "#adi" (into {} (dissoc this :meta)))))

(defmethod print-method Adi
  [v w]
  (.write w (str v)))
