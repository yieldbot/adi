(ns spirit.examples.bkell-test
  (:require [spirit.core.datomic :as datomic]
            [spirit.core [transaction :as transaction]
             [prepare :as prepare]]))

(def data-bkell
  (read-string (slurp "test/data/bkell-default.edn")))

(def schema-bkell
  (read-string (slurp "test/data/bkell-schema.edn")))

(def ds (datomic/connect! "datomic:mem://spirit-examples-bkell" schema-bkell true true))
(datomic/insert! ds data-bkell)



(datomic/update! ds {:book/accounts '_} {:book/accounts {:name "foo" :type :asset :counterWeight :debit}})

(datomic/select ds {:book/accounts '_} :pull {:book {:accounts :checked}})
