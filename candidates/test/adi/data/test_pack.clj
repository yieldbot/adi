(ns adi.data.test-pack
  (:use midje.sweet)
  (:require
   [adi-test.schemas :refer :all]
   [adi-test.checkers :refer :all]
   [hara.collection.hash-map :refer [treeify-keys]]
   [adi.model :refer [model-input]]
   [adi.data.normalise :refer [normalise]]
   [adi.data.pack.analyse :refer [analyse]]))

(defn ->analyse [data env]
  (-> data
      (normalise env)
      (analyse env)))

(->analyse {:person/sibling/name "Sam"}
           {:schema family-links-xm
            :type "datoms"
            })
