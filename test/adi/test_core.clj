(ns adi.test-query
  (:use midje.sweet
        adi.utils)
  (:require [adi.data :as ad]
            [adi.schema :as as]
            [adi.query :as aq]
            [datomic.api :as d]))

(def *uri* "datomic:mem://test-adi-query")
(d/create-database *uri*)
;;  (d/delete-database *uri*)
(def *conn* (d/connect *uri*))




(d/transact )
