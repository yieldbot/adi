(ns adi.data.normalise.test-validate
  (:use midje.sweet)
  (:require
   [adi-test.schemas :refer :all]
   [adi-test.checkers :refer :all]
   [hara.collection.hash-map :refer [treeify-keys]]
   [adi.model :refer [model-input]]
   [adi.data.normalise :as t :refer [normalise]]))


(fact "Simple Convert"
  (t/normalise {:account/name "Chris"}
               {:schema account-name-age-sex-xm
                :model {:validate {:account {:name string?}}}})
  => {:account {:name "Chris"}}

  (t/normalise {:account/name "Chris"}
               {:schema account-name-age-sex-xm
                :model {:validate {:account {:age 1}}}})
  => {:account {:name "Chris"}}

  (t/normalise {:account {:name "Chris"
                          :age "10"}}
               {:schema account-name-age-sex-xm
                :model {:validate {:account {:age #(= % "10")}}}})
  => {:account {:age "10" :name "Chris"}}

  (t/normalise {:account {:name "Chris"
                          :age "10"}}
               {:schema account-name-age-sex-xm
                :model {:validate {:account {:age #(= % "9")}}}})
  => (raises-issue {:not-validated true}))
