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
  ([{:keys [uri schema reset? install-schema?] :as args}]
   (let [pargs (dissoc args :uri :schema :reset? :install-schema?)]
     (connect! uri schema reset? install-schema? pargs)))
  ([uri schema & [reset? install-schema? more]]
      (when reset?
        (datomic/delete-database uri)
        (datomic/create-database uri))

      (let [conn (try (datomic/connect uri)
                      (catch clojure.lang.ExceptionInfo e
                        (if (= (ex-data e) {:db/error :peer/db-not-found})
                          (do (datomic/create-database uri)
                              (datomic/connect uri))
                          (throw e))))
            schema  (if (instance? adi.schema.Schema schema)
                     schema
                     (schema/schema schema))
            _      (if install-schema?
                     (let [dschema (-> schema :flat datomic)]
                       (datomic/transact conn dschema)))]
        (-> (types/map->Adi {:meta {:uri uri :reset? reset? :install-schema? install-schema?}
                             :connection conn
                             :schema schema})
            (merge more)))))
