(ns adi.data.normalise.test-post-mask
  (:use midje.sweet)
  (:require
   [adi-test.schemas :refer :all]
   [adi-test.checkers :refer :all]
   [hara.collection.hash-map :refer [treeify-keys]]
   [adi.model :refer [model-input]]
   [adi.data.normalise :as t :refer [normalise]]))


(fact "Simple Masking"
  (t/normalise {:account/name "Chris"}
               {:schema account-name-age-sex-xm
                :model {:post-mask {:account {:name :checked}}}})
  => {:account {}}

  (t/normalise {:account/name "Chris"}
               {:schema account-name-age-sex-xm
                :model {:post-mask  {:account :checked}}})
  => {}

  (t/normalise {:account {:name "Chris"
                       :age 10}}
               {:schema account-name-age-sex-xm
                :model {:post-mask {:account {:name :checked}}}})
  => {:account {:age 10}})


(fact "Masking with Refs"
  (t/normalise {:account/orders/number 1}
               {:schema account-orders-items-image-xm
                :model {:post-mask {:account {:user :checked}}}})
  => {:account {:orders #{{:number 1}}}}

  (t/normalise {:account/orders/number 1}
               {:schema account-orders-items-image-xm
                :model {:post-mask {:account {:orders {:number :checked}}}}})
  => {:account {:orders #{{}}}}


  (t/normalise {:account/name "Chris"}
               {:schema account-name-age-sex-xm
                :model {:post-mask  {:account :checked}}})
  => {}

  (t/normalise {:account {:name "Chris"
                          :age 10}}
               {:schema account-name-age-sex-xm
                :model {:post-mask {:account {:name :checked}}}})
  => {:account {:age 10}})
