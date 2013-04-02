(ns adi.scratch
 (:use midje.sweet
       adi.utils
       adi.schema
       adi.data)
 (:require [datomic.api :as d]))

(def ^:dynamic *uri* "datomic:mem://scratch")

(d/create-database *uri*)
(def conn (d/connect *uri*))

(d/transact
 conn
 [{:db/id #db/id[:db.part/db]
  :db/ident :person/name
  :db/valueType :db.type/string
  :db/cardinality :db.cardinality/one
  :db/doc "A person's name"
  :db.install/_attribute :db.part/db}
             {:db/id #db/id[:db.part/db -1]
  :db/ident :person/gender
  :db/valueType :db.type/ref
  :db/cardinality :db.cardinality/one
  :db/doc "A person's gender"
  :db.install/_attribute :db.part/db}

 {:db/id #db/id[:db.part/user -2]
  :db/ident :person.gender/female}

 {:db/id #db/id[:db.part/user -3]
  :db/ident :person.gender/male}])

(d/transact
 conn
 [{:db/id #db/id[:db.part/user]
   :person/name "Bob"
   :person/gender :person.gender/male}])

(->>
 (d/q '[:find ?e :where
        [?e :person/gender :person.gender/male]]
      (d/db conn))
 (ffirst)
 (d/entity (d/db conn))
 :person/gender)


(d/q '[:find ?e :where
       [?e :person/name ?g]
       [(not= "Bob" ?g)]]
      (d/db conn))
