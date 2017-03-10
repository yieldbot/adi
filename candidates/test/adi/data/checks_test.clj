(ns adi.data.checks-test
  (:use midje.sweet)
  (:require [adi.data.checks :refer :all]
            [datomic.api :as datomic]))

(def ^:dynamic *url* "datomic:mem://adi.data.checks-test")
(def ^:dynamic *db*
  (let [url  "datomic:mem://adi.data.checks-test"
        _    (datomic/create-database *url*)
        conn (datomic/connect *url*)
        db   (datomic/db conn)]
    (datomic/entity db :db.type/instant)))

^{:refer adi.data.checks/enum? :added "0.3"}
(fact "Returns `true` if `x` is either a keyword or a long"

   (enum? :account.type/vip)   => true
   (enum? :hello/oeuoeu)   => true
   (enum? 1000234343)  => true)

^{:refer adi.data.checks/db-id? :added "0.3"}
(fact "Returns `true` if `x` is of type `datomic.db.DbId"

  (db-id? (datomic/tempid :db.user))
  => true)

^{:refer adi.data.checks/entity? :added "0.3"}
(fact "Returns `true` if `x` is of type `datomic.query.EntityMap"

  (entity? (let [url  "datomic:mem://adi.data.checks-test"
                 _    (datomic/create-database *url*)
                 conn (datomic/connect *url*)
                 db   (datomic/db conn)]
             (datomic/entity db :db.type/instant)))
  => true)

^{:refer adi.data.checks/ref? :added "0.3"}
(fact "Returns `true` if `x` is either a hash-map, long, db-id or entity"

  (ref? 100000234343) => true
  (ref? (datomic/tempid :db.user)))

^{:refer adi.data.checks/vexpr? :added "0.3"}
(fact "checks whether an input is a vector expression"
  (vexpr? [[":hello"]]) => true)