(ns adi.schema.types
  (:require [hara.common :refer [long? hash-map?]]))

(defn enum?
  "Returns `true` if `x` is an enum keyword"
  [x] (or (keyword? x) (long? x)))

(defn db-id?
  "Returns `true` if `x` implements `datomic.db.DbId"
  [x]  (instance? datomic.db.DbId x))

(defn entity?
  "Returns `true` if `x` is of type `datomic.query.EntityMap`."
  [x] (instance? datomic.query.EntityMap x))

(defn ref?
  "Returns `true` if `x` implements `clojure.lang.APersistentMap`
   or is of type `datomic.query.EntityMap` or is a long or db-id."
  [x] (or (hash-map? x) (entity? x) (db-id? x) (long? x)))
