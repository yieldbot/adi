(ns adi.process.analyse-test
  (:use midje.sweet)
  (:require [adi.process.analyse :refer :all]
            [adi.schema :as schema]
            [adi.test.examples :as examples]
            [adi.test.checkers :refer :all]))

(fact
  (analyse {:account {:name "Chris"}}
           {:schema (schema/schema examples/account-name-age-sex)
            :options {:auto-ids false}})
  => {:account/name "Chris"}

  (analyse {:account {:name :Chris
                      :age "10"}}
           {:schema (schema/schema examples/account-name-age-sex)
            :options {:auto-ids false}})
  => {:account/name "Chris", :account/age 10}

  (analyse {:account {:name :Chris
                      :hello "world"}}
           {:schema (schema/schema examples/account-name-age-sex)
            :options {:auto-ids false}})
  => (raises-issue {:no-schema true
                    :data "world",
                    :nsv [:account :hello]}))
