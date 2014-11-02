(ns adi.examples.bkell-test
  (:use midje.sweet)
  (:require [adi.core :as adi]))

(def data-bkell
  (read-string (slurp "test/data/bkell-default.edn")))

(def schema-bkell
  (read-string (slurp "test/data/bkell-schema.edn")))

(def ds (adi/connect! "datomic:mem://adi-examples-bkell" schema-bkell true true))
(adi/insert! ds data-bkell)
