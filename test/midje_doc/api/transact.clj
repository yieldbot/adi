(ns midje-doc.api.transact
  (:use midje.sweet)
  (:require [adi.core :as adi]
            [datomic.api :as datomic]))

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

(def ds (adi/connect! "datomic:mem://adi-examples-step-1" schema-1 true true))

(fact "select and delete! should be the same"

  (let [trans-res (set (apply concat (for [i (range 5)]
                                       (adi/transact! ds [{:db/id (datomic/tempid :db.part/user)
                                                           :account/credits 10}]))))
        select-res (adi/select ds :account/credits :ids)
        delete-res (adi/delete! ds :account/credits)]
    select-res => delete-res
    select-res => trans-res))


(fact "update! should do the same thing"

  (let [delete-res (adi/delete! ds :account/credits)
        trans-res (set (for [i (range 5)]
                         (adi/transact! ds [{:db/id (datomic/tempid :db.part/user)
                                             :account/credits 10}])))
        update-res (adi/update! ds :account/credits {:account/password "pass2"})
        select-res (adi/select ds :account/credits :ids
                               :return {:account {:credits :unchecked}})]
    select-res => (set update-res)))
