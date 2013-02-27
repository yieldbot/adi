(ns adi.test-query
  (:use midje.sweet
        adi.utils
        adi.core)
  (:require [adi.data :as ad]
            [adi.schema :as as]
            [datomic.api :as d]))

(def *uri* "datomic:mem://test-adi-query")
(d/create-database *uri*)
(comment (d/delete-database *uri*))
(def $ds (ds *uri* ))




(transact )
