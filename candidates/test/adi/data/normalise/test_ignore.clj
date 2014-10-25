(ns adi.data.normalise.test-ignore
  (:use midje.sweet)
  (:require
   [adi-test.schemas :refer :all]
   [adi-test.checkers :refer :all]
   [hara.collection.hash-map :refer [treeify-keys]]
   [adi.model :refer [model-input]]
   [adi.data.normalise :as t :refer [normalise]]))


(fact "Simple Ignore"
  (t/normalise {:account {:name "Chris"
                       :age 10
                       :parents ["henry" "sally"]}}
             {:schema account-name-age-sex-xm})
  => (raises-issue {:key-path [:account],
                    :normalise true,
                    :nsv [:account :parents],
                    :no-schema true})


  (t/normalise {:account {:name "Chris"
                       :age 10
                       :parents ["henry" "sally"]}}
               {:schema account-name-age-sex-xm
                :model {:ignore {:account {:parents :checked}}}})
  => {:account {:name "Chris"
                :age 10
                :parents ["henry" "sally"]}})
