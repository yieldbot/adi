(ns adi.core-test
  (:use midje.sweet)
  (:require [adi.core
             [connection :refer [connect! disconnect!]]
             [select :refer [select]]
             [transaction :refer [insert! transact! delete! update!]]]
            [hara.data.nested :refer [merge-nested]]
            [datomic.api :as datomic]))

(fact "first end-to-end insert and select"
  (def adi (connect! "datomic:mem://adi-core-test"
                     {:account/name [{:representative true}]} true true))

  (insert! adi [{:account/name "Chris"} {:account/name "Bob"}] nil)
  (select adi {:account/name '_} nil)
  => #{{:account {:name "Bob"}} {:account {:name "Chris"}}}

  (select adi :account/name nil)
  => #{{:account {:name "Bob"}} {:account {:name "Chris"}}}

  (select adi :account nil)
  => #{{:account {:name "Bob"}} {:account {:name "Chris"}}}

  (select adi :account {:options {:ids true}})
  => #{{:db {:id 17592186045418}, :account {:name "Chris"}}
          {:db {:id 17592186045419}, :account {:name "Bob"}}}

  (select adi :account {:options {:return-ids true}})
  => #{17592186045418 17592186045419}

  (delete! adi #{:account} nil)
  (select adi :account {:options {:return-ids true}})
  => #{})
