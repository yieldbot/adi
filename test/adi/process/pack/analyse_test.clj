(ns adi.process.pack.analyse-test
  (:use midje.sweet)
  (:require [adi.process.pack.analyse :refer :all]
            [adi.schema :as schema]
            [adi.test.examples :as examples]
            [adi.test.checkers :refer :all]))

^{:refer adi.process.pack.analyse/analyse-raw :added "0.3"}
(fact "turns a nested-tree map into reference maps"
  (analyse-raw {:account {:name "Chris"}}
           {:schema (schema/schema examples/account-name-age-sex)
            :options {:auto-ids false}})
  => {:account/name "Chris"}

  (analyse-raw {:account {:name :Chris
                      :age "10"}} ;; auto coercion
           {:schema (schema/schema examples/account-name-age-sex)
            :options {:auto-ids false}})
  => {:account/name "Chris", :account/age 10}
  ^:hidden
  ["raises an error on wrong schema unless otherwise stated"]
  (analyse-raw {:account {:name :Chris
                          :hello "world"}}
           {:schema (schema/schema examples/account-name-age-sex)
            :options {:auto-ids false}})
  => {:account/name "Chris"}

  (analyse-raw {:account {:name :Chris
                      :hello "world"}}
         {:schema (schema/schema examples/account-name-age-sex)
          :options {:auto-ids false
                    :schema-ignore true}})
  => {:account/name "Chris"}

  "control of db/id leaks"
  (analyse-raw {:db {:id '?y}
            :account {:name '?x}}
           {:schema (schema/schema examples/account-name-age-sex)})
  => '{:# {:id ?y} :account/name ?x}

  (analyse-raw {:db {:id 2}
            :account {:name '?x}}
           {:schema (schema/schema examples/account-name-age-sex)
            :options {:ban-ids true}})
  => (raises-issue {:id-banned true :id 2})

  (analyse-raw {:db {:id 2}
            :account {:name '?x}}
           {:schema (schema/schema examples/account-name-age-sex)
            :options {:ban-body-ids true}})
  => {:# {:id 2} :account/name '?x}

  (analyse-raw {:account {:user "Chris"
                      :orders #{{:+ {:db {:id 3}}
                                 :number 1}}}}
           {:schema (schema/schema examples/account-orders-items-image)
            :options {:ban-body-ids true}})
  => (raises-issue {:body-id-banned true :id 3})

  (analyse-raw {:account {:user "Chris"
                      :orders #{100034}}}
           {:schema (schema/schema examples/account-orders-items-image)
            :options {:ban-body-ids true}})
  => (raises-issue {:body-id-banned true :id 100034})

  "control of expressions"
  (analyse-raw {:account {:user '(= "Chris")}}
           {:schema (schema/schema examples/account-orders-items-image)
            :options {:auto-ids false}})
  => {:account/user '(= "Chris")}

  (analyse-raw {:account {:user '(= "Chris")}}
           {:schema (schema/schema examples/account-orders-items-image)
            :options {:ban-expressions true}})
  => (raises-issue {:expression-banned true :data '(= "Chris")})

  (analyse-raw {:account {:orders #{{:number '(= 5)}}}}
           {:schema (schema/schema examples/account-orders-items-image)
            :options {:ban-expressions true}})
  => (raises-issue {:expression-banned true :data '(= 5)})

  "reverse analysis"
  (analyse-raw {:order {:account #{{:user "Chris"}}}}
           {:schema (schema/schema examples/account-orders-items-image)
            :options {:auto-ids false}})
  => {:order/account #{{:account/user "Chris"}}})
