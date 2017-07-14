(ns spirit.core.datomic.types-test
  (:use hara.test)
  (:require [spirit.core.datomic.types :refer :all]
            [spirit.schema :as schema]
            [datomic.api :as datomic]
            [hara.component :as component]))

^{:refer spirit.core.datomic.types/construct-uri :added "0.5"}
(fact "constructs the datomic uri"

  (construct-uri {:protocol :mem
                  :name "hello"})
  => "datomic:mem://hello"

  (construct-uri {:protocol :dev
                  :host "localhost"
                  :port 4334
                  :name "hello"})
  => "datomic:dev://localhost:4334/hello"

    (construct-uri {:uri "datomic:mem://hello"})
  => "datomic:mem://hello")

^{:refer spirit.core.datomic.types/connect :added "0.5"}
(fact "connect to uri, creating the database if it does not exist"

  (connect "datomic:mem://hello")
  => #(instance? datomic.peer.LocalConnection %))

^{:refer spirit.core.datomic.types/delete-database :added "0.5"}
(fact "deletes the specified database"
  
  (delete-database {:uri "datomic:mem://hello"}))

^{:refer spirit.core.datomic.types/install-schema :added "0.5"}
(comment "installs the schema to the database"
  
  (-> (map->Datomic {:schema {:account {:user [{}]}}
                     :connection  (connect "datomic:mem://hello")})
      (install-schema))
  ;;=> #datomic{:schema #schema{:account {:user :string}}
  ;;            :connection #conn{...}
  )

^{:refer spirit.core.datomic.types/last-transaction-time :added "0.5"}
(fact "returns the time of last transaction for the database"
  
  (last-transaction-time (datomic/db (connect "datomic:mem://hello")))
  => (contains [number? #(instance? java.util.Date %)]))

^{:refer spirit.core.datomic.types/start-datomic :added "0.5"}
(comment "helper for IComponent/-start"
  
  (start-datomic {:schema {:account {:user [{}]}}
                  :uri "datomic:mem://hello"
                  :options #{:install-schema}})
  ;;=> {:schema #schema{:account {:user :string}},
  ;;    :uri "datomic:mem://hello",
  ;;    :options #{:install-schema},
  ;;    :connection #conn{...}}
  )

^{:refer spirit.core.datomic.types/stop-datomic :added "0.5"}
(comment "helper for IComponent/-start"

  (-> {:uri "datomic:mem://hello"}
      (start-datomic)
      (stop-datomic)
      (delete-database))
  ;;=> {:uri "datomic:mem://hello", :schema #schema{}}
  )

^{:refer spirit.core.datomic.types/Datomic :added "0.5"}
(fact "testing IComponent functionality"

  (def topology {:db [map->Datomic :schema]
                 :schema [schema/schema]})
  
  (def config   {:db  {:protocol :mem
                       :name "spirit.core.datomic.types-test"}
                 :schema {:account {:name [{}]}}})
  
  (-> (component/system topology config)
      (component/start)
      (component/stop)
      :db
      (delete-database)))

(comment
  (require 'lucid.unit)
  (lucid.unit/import)
  )
