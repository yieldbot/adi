(ns adi.data.normalise.test-pre-require
  (:use midje.sweet)
  (:require
   [adi-test.schemas :refer :all]
   [adi-test.checkers :refer :all]
   [adi.model :refer [model-input]]
   [adi.data.normalise :as t :refer [normalise]]))

(fact "Simple Pre Require"
  (t/normalise {:account/name "Chris"}
            {:schema account-name-age-sex-xm
             :model {:pre-require {:account {:name :checked}}}})
  => {:account {:name "Chris"}}

  (t/normalise {:account/name "Chris"}
            {:schema account-name-age-sex-xm
             :model {:pre-require {:account {:name :checked}}
                     :pre-mask   {:account {:name :checked}}}})
  => {:account {}})

(fact "Refs Pre Require"
  (t/normalise {:account/orders #{{:number 1}
                                  {:number 2}}}
              {:schema account-orders-items-image-xm
               :model {:pre-require {:account {:orders {:number :checked}}}}})
  => {:account {:orders #{{:number 1}
                          {:number 2}}}})
