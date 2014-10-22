(ns adi.data.normalise.test-convert
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
                :model {:convert {:account {:name (fn [x] (.toLowerCase x))}}}})
  => {:account {:name "chris"}}

  (t/normalise {:account/name "Chris"}
               {:schema account-name-age-sex-xm
                :model {:convert  {:account {:age  1}}}})
  => {:account {:name "Chris"}}

  (t/normalise {:account {:name "Chris"
                          :age "10"}}
               {:schema account-name-age-sex-xm
                :model {:convert {:account {:age (fn [x] (Long/parseLong x))}}}})
  => {:account {:age 10 :name "Chris"}}

  (t/normalise {:account {:name "Chris"
                       :age "10"}}
               {:schema account-name-age-sex-xm
                :model {:pre-mask {:account {:name :checked}}
                        :convert {:account {:age (fn [x] (Long/parseLong x))}}}})
  => {:account {:age 10}})
