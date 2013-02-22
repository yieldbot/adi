(ns anadi.test-query
  (:use midje.sweet
        anadi.utils)
  (:require [anadi.data :as ad]
            [anadi.schema :as as]
            [anadi.query :as aq]
            [datomic.api :as d]))

(def *uri* "datomic:mem://test-anadi-query")
(d/create-database *uri*)
;;  (d/delete-database *uri*)
(def *conn* (d/connect *uri*))




(d/transact )
