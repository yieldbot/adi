(ns adi.examples.bkell-test
  ;;(:use midje.sweet)
  (:require [adi.core :as adi]))

(def data-bkell
  (read-string (slurp "test/data/bkell-default.edn")))

(def schema-bkell
  (read-string (slurp "test/data/bkell-schema.edn")))

(def ds (adi/connect! "datomic:mem://adi-examples-bkell" schema-bkell true true))
(adi/insert! ds data-bkell)

#_(./pprint (adi/select ds :system/groups :pull {:system {:groups {:books {:accounts :checked}
                                                                 :users :checked
                                                                 :owner :checked}}} :ids))


(adi/select ds 17592186045456)

(adi/select ds :books/accounts :pull {:books {:accounts :checked}})
#{{:books {:accounts #{{:type :revenue, :counterWeight :credit, :name "revenue"}
                       {:type :asset, :counterWeight :debit, :name "foo"}
                       {:type :liability, :counterWeight :credit, :name "debt"}
                       {:type :asset, :counterWeight :debit, :name "cash"}
                       {:type :expense, :counterWeight :debit, :name "expense"}}}}}

(let [id (adi/select ds :books/accounts :return :ids :first)]
  (println id)
  (adi/insert! ds {:account {:books id
                             :name "foo"
                             :type :asset
                             :counterWeight :debit}}))
