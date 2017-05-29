(ns spirit.core.connection
  (:require [datomic.api :as datomic]
            [spirit.schema :as schema]
            [spirit.schema.datomic :refer [datomic]]
            [spirit.core.types :as types]))

(defn disconnect! [spirit]
  (if-let [conn (:connection spirit)]
    (datomic/release conn))
  (dissoc spirit :connection))

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
            schema  (if (instance? spirit.schema.Schema schema)
                     schema
                     (schema/schema schema))
            _      (if install-schema?
                     (let [dschema (-> schema :flat datomic)]
                       (datomic/transact conn dschema)))]
        (-> (types/map->Adi {:meta {:uri uri :reset? reset? :install-schema? install-schema?}
                             :connection conn
                             :schema schema})
            (merge more)))))
