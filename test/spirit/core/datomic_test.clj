(ns spirit.core.datomic-test
  (:use hara.test)
  (:require [spirit.core.datomic :refer :all]
            [spirit.common.graph :as graph]
            [hara.component :as component]))

^{:refer spirit.core.datomic/connect! :added "0.5"}
(fact "creates a datomic connection to a database"

  (def ds (connect! {:protocol :mem
                     :name "spirit.core.datomic-test"
                     :schema {:account {:user [{:required true}]}}
                     :options {:ids false
                               :reset-db true
                               :install-schema true}})))

^{:refer spirit.core.datomic/disconnect! :added "0.5"}
(comment "releases the datomic connection"

  (def ds (connect! "datomic:mem://spirit.core.datomic-test"
                    {:account {:user [{:required true}]}}
                    true true))
  (disconnect! ds))


^{:refer spirit.core.datomic/args->opts :added "0.5"}
(fact "makes a map from input arguments"

  (args->opts [:raw false :first :ids :transact :datasource])
  => {:options {:raw false,
                :first true,
                :ids true},
      :transact :datasource})

^{:refer spirit.core.datomic/sync-> :added "0.5"}
(fact "makes a map from input arguments"
  
  (-> (test-instance)
      (insert! [{:account/email "foo"}
                {:account {:email "bar" :firstname "baz"}}]
               {:transact :datasource})
      (sync-> {:transact :datasource}
              (insert! {:account/email "baz"})
              (update!  {:account/email "bar"} {:account/lastname "foobar"})
              (retract! {:account/email "bar"} [:account/firstname])
              (delete! {:account/email "foo"}))
      (select {:account/email '_} {:options {:ids false}}))
  => #{{:account {:email "bar", :lastname "foobar"}}
       {:account {:email "baz"}}})

^{:refer spirit.core.datomic/create-function-template :added "0.5"}
(comment "helper function to define-database-functions")

^{:refer spirit.core.datomic/define-database-functions :added "0.5"}
(comment "scaffolding to create user friendly datomic functions")

^{:refer spirit.core.datomic/create-data-form :added "0.5"}
(comment "helper function to sync-> for creating datom outputs")

^{:refer spirit.core.datomic/test-instance :added "0.5"}
(comment "creates a test in-memory database")

^{:refer spirit.common.graph/grapd :added "0.5"}
(fact "interface to graph/db"
  
  (graph/graph {:type :datomic
                :protocol :mem
                :name "spirit.core.datomic-test"
                :options {:reset-db true}})
  => spirit.core.datomic.types.Datomic)

^{:refer spirit.common.graph/-select :added "0.5"}
(fact "selects from the database"

  (-> (test-instance)
      (graph/insert [{:account/email "hello"}
                     {:account/email "world"}])
      (graph/select {:account {:email "hello"}}
                    {:options {:first true}}))
  => (contains-in {:account {:email "hello"}}))

^{:refer spirit.common.graph/-empty :added "0.5"}
(comment "inserts into the database"

  (-> (test-instance)
      (graph/insert {:account {:email "hello"
                               :firstname "chris"
                               :lastname "zheng"}})
      (graph/empty)
      (graph/select {:account/email '_}))
  => #{})

^{:refer spirit.common.graph/-insert :added "0.5"}
(comment "inserts into the database"

  (-> (test-instance)
      (graph/insert {:account {:email "hello"
                               :firstname "chris"
                               :lastname "zheng"}})))

^{:refer spirit.common.graph/-delete :added "0.5"}
(fact "deletes from the database"

  (-> (test-instance)
      (graph/insert [{:account/email "hello"}
                     {:account/email "world"}])
      (graph/delete {:account/email "hello"})
      (graph/select {:account/email '_})
      count)
  => 1)

^{:refer spirit.common.graph/-retract :added "0.5"}
(fact "retracts keys from the selected entity"

  (-> (test-instance)
      (graph/insert {:account {:email "hello"
                               :firstname "chris"
                               :lastname "zheng"}})
      (graph/retract {:account/email "hello"}
                     [:account/firstname])
      (graph/select {:account/email '_}
                    {:options {:first true
                               :ids false}}))
  => {:account {:email "hello", :lastname "zheng"}})

^{:refer spirit.common.graph/-update :added "0.5"}
(fact "retracts keys from the selected entity"

  (-> (test-instance)
      (graph/insert {:account {:email "hello"
                               :firstname "chris"
                               :lastname "zheng"}})
      (graph/update {:account/email "hello"}
                    {:account/email "world"})
      (graph/select {:account/email '_}
                    {:options {:first true
                               :ids false}}))
  => {:account {:email "world", :firstname "chris", :lastname "zheng"}})
