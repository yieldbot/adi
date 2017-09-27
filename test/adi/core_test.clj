(ns adi.core-test
  (:use midje.sweet)
  (:require [adi.core :refer :all]
            [datomic.api :as datomic]))

(comment
  (fact "first end-to-end insert and select"
    (def adi (connect! "datomic:mem://adi-core-test"
                       {:account/name [{:representative true}]} true true))

    (insert! adi [{:account/name "Chris"} {:account/name "Bob"}])
    (select adi {:account/name '_})
    => #{{:account {:name "Bob"}} {:account {:name "Chris"}}}

    (select adi :account/name)
    => #{{:account {:name "Bob"}} {:account {:name "Chris"}}}

    (select adi :account)
    => #{{:account {:name "Bob"}} {:account {:name "Chris"}}}

    (select adi :account :ids)
    => #{{:db {:id 17592186045418}, :account {:name "Chris"}}
         {:db {:id 17592186045419}, :account {:name "Bob"}}}

    (select adi :account :return :ids)
    => #{17592186045418 17592186045419}

    (delete! adi #{:account})
    (select adi :account :return :ids)
    => #{}))
