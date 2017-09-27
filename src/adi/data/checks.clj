(ns adi.data.checks
  (:require [hara.common.checks :refer [long? hash-map?]]))

(defn enum?
  "Returns `true` if `x` is either a keyword or a long

   (enum? :account.type/vip)   => true
   (enum? :hello/oeuoeu)   => true
   (enum? 1000234343)  => true"
  {:added "0.3"}
  [x] (or (keyword? x) (long? x)))

(defn db-id?
  "Returns `true` if `x` is of type `datomic.db.DbId

  (db-id? (datomic/tempid :db.user))
  => true"
  {:added "0.3"}
  [x]  (instance? datomic.db.DbId x))

(defn entity?
  "Returns `true` if `x` is of type `datomic.query.EntityMap

  (entity? (let [url  \"datomic:mem://adi.data.checks-test\"
                 _    (datomic/create-database *url*)
                 conn (datomic/connect *url*)
                 db   (datomic/db conn)]
             (datomic/entity db :db.type/instant)))
  => true"
  {:added "0.3"}
  [x] (instance? datomic.query.EntityMap x))

(defn ref?
  "Returns `true` if `x` is either a hash-map, long, db-id or entity

  (ref? 100000234343) => true
  (ref? (datomic/tempid :db.user))"
  {:added "0.3"}
  [x] (or (hash-map? x) (entity? x) (db-id? x) (long? x)))
  
(defn vexpr?
  "checks whether an input is a vector expression
  (vexpr? [[\":hello\"]]) => true"
  {:added "0.3"}
  [v]
  (and (vector? v)
       (= 1 (count v))
       (vector? (first v))
       (= 1 (count (first v)))))