(ns adi.data.test-normalise
  (:use midje.sweet)
  (:require
   [adi-test.schemas :refer :all]
   [adi-test.checkers :refer :all]
   [ribol.core :refer [manage continue on]]
   [hara.collection.hash-map :refer [treeify-keys]]
   [adi.schema :refer [make-xm]]
   [adi.data.normalise :as t :refer [normalise]]))
   
   (fact (+ 1 1) => 2)