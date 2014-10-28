(ns adi.examples.step-1
  [:use midje.sweet]
  (:require [adi.core :as adi]
            [adi.test.checkers :refer :all]))

[[:section {:title "Step 1 - Schema Basics"}]]

"The fundamental building "

(def schema-1
  {:account/user     [{:type :string      ;; (1)
                       :cardinality :one
                       :unique :value     ;; (2)
                       :required true}]   ;; (3)
   :account/password [{:required true     ;; (1) (3)
                       :restrict ["password needs an integer to be in the string"
                                  #(re-find #"\d" %)]}] ;; (4)
   :account/credits  [{:type :long
                       :default 0}]})

"There are a couple of things to note about our definitions the entry.

  1. We specified the `:type` for `:account/user` to be `:string` and `:cardinality` to be `:one`. However, because these are default options, we can optionally leave them out for `:account/password`.
  * We want the value of `:account/user` to be unique.
  * We require that both `:account/user` and `:account/password` to be present on insertion.
  * We are checking that `:account/password` contains at least one number
  * We set the default amount of `:account/credits` to be `0`

Now, we construct a datastore."

(fact "restrictions and checks"
  (def ds (adi/connect! "datomic:mem://adi-examples-step-1" schema-1 true true))

  (adi/insert! ds {:account {:credits 10}})
  => (throws Exception "The following keys are required: #{:account/password :account/user}")

  (adi/insert! ds {:account {:user "adi"}})
  => (throws Exception "The following keys are required: #{:account/password}")

  (adi/insert! ds {:account {:user "adi" :password "hello"}})
  => (raises-issue {:failed-restriction true})

  (adi/insert! ds {:account {:user "adi" :password "hello1" :type :vip}})
  => (raises-issue {:nsv [:account :type] :no-schema true}))

(fact "uniqueness"
  (def ds (adi/connect! "datomic:mem://adi-examples-step-1" schema-1 true true))

  (adi/insert! ds {:account {:user "adi" :password "hello1"}})
  (adi/insert! ds {:account {:user "adi" :password "hello2" :credits 10}})
  => throws

  (adi/insert! ds {:account {:user "adi2" :password "hello2" :credits 10}})
  (adi/select ds :account)
  => #{{:account {:user "adi", :password "hello1", :credits 0}}
       {:account {:user "adi2", :password "hello2", :credits 10}}})

(fact "selections through history"
  (adi/select ds :account :ids)
  => #{{:db {:id 17592186045418}, :account {:user "adi", :password "hello1", :credits 0}}
       {:db {:id 17592186045420}, :account {:user "adi2", :password "hello2", :credits 10}}}

  (adi/transactions ds :account/user)
  => [1001 1003]

  (adi/transactions ds :account/user "adi")
  => [1001]

  (adi/select ds :account :at 1001)
  => #{{:account {:user "adi", :password "hello1", :credits 0}}})
