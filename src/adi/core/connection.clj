(ns adi.core.connection
  (:require [datomic.api :as datomic]
            [adi.schema :as schema]
            [adi.schema.datomic :refer [datomic]]
            [adi.core.types :as types]))

(defn disconnect! [adi]
  (if-let [conn (:connection adi)]
    (datomic/release conn))
  (dissoc adi :connection))

(defn connect!
  ([adi]
     (disconnect! adi)
     (let [{:keys [uri reset? install-schema?]} (:meta adi)]
       (connect! uri (:schema adi) reset? install-schema?)))
  ([uri schema & [reset? install-schema?]]
      (when reset?
        (datomic/delete-database uri)
        (datomic/create-database uri))

      (let [conn (try (datomic/connect uri)
                      (catch clojure.lang.ExceptionInfo e
                        (if (= (ex-data e) {:db/error :peer/db-not-found})
                          (do (datomic/create-database uri)
                              (datomic/connect uri))
                          (throw e))))
            schema (if (instance? adi.schema.Schema schema)
                     schema
                     (schema/schema schema))
            _      (if install-schema?
                     (let [dschema (-> schema :flat datomic)]
                       (datomic/transact conn dschema)))]
        (types/map->Adi {:meta {:uri uri :reset? reset? :install-schema? install-schema?}
                   :connection conn
                   :schema schema}))))