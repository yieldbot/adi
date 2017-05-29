(ns spirit.examples.bkell-test
  (:require [spirit.core :as spirit]
            [spirit.core [transaction :as transaction]
             [prepare :as prepare]]))

(def data-bkell
  (read-string (slurp "test/data/bkell-default.edn")))

(def schema-bkell
  (read-string (slurp "test/data/bkell-schema.edn")))

(def ds (spirit/connect! "datomic:mem://spirit-examples-bkell" schema-bkell true true))
(spirit/insert! ds data-bkell)



(spirit/update! ds {:book/accounts '_} {:book/accounts {:name "foo" :type :asset :counterWeight :debit}})

(spirit/select ds {:book/accounts '_} :pull {:book {:accounts :checked}})
