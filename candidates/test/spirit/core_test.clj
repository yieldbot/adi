(ns spirit.core-test
  (:use hara.test)
  (:require [spirit.core :refer :all]
            [datomic.api :as datomic]))

(comment
  (fact "first end-to-end insert and select"
    (def spirit (connect! "datomic:mem://spirit-core-test"
                       {:account/name [{:representative true}]} true true))

    (insert! spirit [{:account/name "Chris"} {:account/name "Bob"}])
    (select spirit {:account/name '_})
    => #{{:account {:name "Bob"}} {:account {:name "Chris"}}}

    (select spirit :account/name)
    => #{{:account {:name "Bob"}} {:account {:name "Chris"}}}

    (select spirit :account)
    => #{{:account {:name "Bob"}} {:account {:name "Chris"}}}

    (select spirit :account :ids)
    => #{{:db {:id 17592186045418}, :account {:name "Chris"}}
         {:db {:id 17592186045419}, :account {:name "Bob"}}}

    (select spirit :account :return :ids)
    => #{17592186045418 17592186045419}

    (delete! spirit #{:account})
    (select spirit :account :return :ids)
    => #{}))
