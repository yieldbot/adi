(ns adi.examples.bkell-test
  (:require [adi.core :as adi]
            [adi.core [transaction :as transaction]
             [prepare :as prepare]]))

(def data-bkell
  (read-string (slurp "test/data/bkell-default.edn")))

(def schema-bkell
  (read-string (slurp "test/data/bkell-schema.edn")))

(def ds (adi/connect! "datomic:mem://adi-examples-bkell" schema-bkell true true))
(adi/insert! ds data-bkell)



(adi/update! ds {:book/accounts '_} {:book/accounts {:name "foo" :type :asset :counterWeight :debit}})

(adi/select ds {:book/accounts '_} :pull {:book {:accounts :checked}})
