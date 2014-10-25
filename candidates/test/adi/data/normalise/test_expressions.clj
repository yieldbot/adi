(ns adi.data.normalise.test-expressions
  (:use midje.sweet)
  (:require
   [adi-test.schemas :refer :all]
   [adi-test.checkers :refer :all]
   [hara.collection.hash-map :refer [treeify-keys]]
   [adi.model :refer [model-input]]
   [adi.data.normalise :as t :refer [normalise]]))


 (fact "Simple Expressions"
   (t/normalise {:account/name '(= "Chris")}
                {:schema account-name-age-sex-xm
                 :model {:expressions {:account {:name '(= "Chris")}}}})
   => {:account {:name '(= "Chris")}}

   (t/normalise {:account/name '(= "Chris")}
                {:schema account-name-age-sex-xm
                 :model {:expressions {:account {:name '#{=}}}}})
   => {:account {:name '(= "Chris")}}

   (t/normalise {:account/name '(= "Chris")}
                {:schema account-name-age-sex-xm
                 :model {:expressions {:account {:name #{'(= _)}}}}})
   => {:account {:name '(= "Chris")}}

   (t/normalise {:account/name '(= "Chris")}
                {:schema account-name-age-sex-xm
                 :model {:expressions {:account {:name #{'(= string?)}}}}})
   => {:account {:name '(= "Chris")}}

   (t/normalise {:account/name '(= "Chris")}
                {:schema account-name-age-sex-xm
                 :model {:expressions {:account {:name '_}}}})
   => {:account {:name '(= "Chris")}}

   (t/normalise {:account/name "Chris"}
                {:schema account-name-age-sex-xm
                 :model {:expressions {:account {:name '_}}}})
   => {:account {:name "Chris"}}

   (t/normalise {:account/name '(= "Chris")}
                {:schema account-name-age-sex-xm
                 :model {:expressions {:account {:name nil}}}})
   => (raises-issue {:failed-check true})

   (t/normalise {:account/name '(= "Chris")}
                {:schema account-name-age-sex-xm
                 :model {:expressions {}}})
   => (raises-issue {:failed-check true}))
