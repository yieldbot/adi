(ns adi.examples.step-2
  (:use midje.sweet)
  (:require [adi.core :as adi]
            [adi.core.select :as select]
            [adi.test.checkers :refer :all]
            [datomic.api :as datomic]))

[[:section {:title "Step 2 - Enums and Query"}]]

(def schema-2
  {:account {:user     [{:required true
                         :unique :value}]
             :password [{:required true
                         :restrict ["password needs an integer to be in the string"
                                    #(re-find #"\d" %)]}]
             :credits  [{:type :long
                         :default 0}]
             :type     [{:type :enum        ;; (2)
                         :default :free
                         :enum {:ns :account.type
                                :values #{:admin :free :paid}}}]}})

(fact
  (def ds (adi/connect! "datomic:mem://adi-examples-step-2" schema-2 true true))

  (adi/insert! ds {:account {:user "adi1"          ;; (1)
                             :password "hello1"}
                   :account/type :paid})
  (adi/select ds :account)
  => #{{:account {:type :paid, :user "adi1", :password "hello1", :credits 0}}}

  (adi/insert! ds [{:account {:password "hello2"
                              :type :account.type/admin}  ;; (2)
                    :account/user "adi2"}
                   {:account {:user "adi3"
                              :credits 1000}
                    :account/password "hello3"}])

  (adi/select ds :account)
  => #{{:account {:user "adi1", :password "hello1", :credits 0, :type :paid}}
       {:account {:user "adi2", :password "hello2", :credits 0, :type :admin}}
       {:account {:user "adi3", :password "hello3", :credits 1000, :type :free}}}


  (adi/insert! ds {:account {:user "adi4"
                             :password "hello4"
                             :type :vip}})
  => throws)

(fact "Different Selection"

  (adi/select ds {:account/type :admin} :first)
  => {:account {:user "adi2", :password "hello2", :credits 0, :type :admin}}

  (adi/select ds {:account/credits 1000} :first)
  => {:account {:user "adi3", :password "hello3", :credits 1000, :type :free}}

  (adi/select ds {:account/credits '(> 10)} :first)
  => {:account {:user "adi3", :password "hello3", :credits 1000, :type :free}}

  (adi/select ds {:account/credits '(> ? 10)} :first)
  => {:account {:user "adi3", :password "hello3", :credits 1000, :type :free}}

  (adi/select ds {:account/credits '(< 10 ?)} :first)
  => {:account {:user "adi3", :password "hello3", :credits 1000, :type :free}}

  (adi/select ds {:account/user '(.contains "2")} :first)
  => {:account {:user "adi2", :password "hello2", :credits 0, :type :admin}}

  (adi/select ds {:account/user '(.contains ? "2")} :first)
  => {:account {:user "adi2", :password "hello2", :credits 0, :type :admin}}

  (adi/select ds {:account/user '(.contains "adi222" ?)} :first)
  => {:account {:user "adi2", :password "hello2", :credits 0, :type :admin}})

(fact "Historical Data"
  (adi/transactions ds :account/user)
  => [1004 1006]

  (adi/select ds :account :at 1004)
  => #{{:account {:user "adi1", :password "hello1", :credits 0, :type :paid}}})

(comment
  (select/select ds :account {:at 1004
                              :options {:first true}}))
