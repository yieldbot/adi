(ns adi.core-test
  (:use midje.sweet)
  (:require [adi.core
             [connection :refer [connect! disconnect!]]
             [select :refer [select]]
             [transaction :refer [insert! transact! delete! update!]]]))

(fact "first end-to-end insert and select"
  (let [adi (connect! "datomic:mem://adi-core-test" {:account/name [{}]} true true)]
   (insert! adi [{:account/name "Chris"} {:account/name "Bob"}])
   (select adi {:account/name '_})
   => #{{:account {:name "Bob"}} {:account {:name "Chris"}}}))
