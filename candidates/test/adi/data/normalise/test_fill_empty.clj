(ns adi.data.normalise.test-fill-empty
  (:use midje.sweet)
  (:require
   [adi-test.schemas :refer :all]
   [adi-test.checkers :refer :all]
   [adi.model :refer [model-input]]
   [adi.data.normalise :as t :refer [normalise]]))


(fact "Simple Fill"
  (t/normalise {:account/name "Chris"}
            {:schema account-name-age-sex-xm
             :model {:fill-empty {:account {:age 10}}}})
  => {:account {:name "Chris", :age 10}}

  (t/normalise {:account/name "Chris"}
            {:schema account-name-age-sex-xm
             :model {:fill-empty {:account {:age (fn [refs env]
                                                   (:age env))}}}
             :age 10})
  => {:account {:name "Chris", :age 10}}

  (t/normalise {:db/id 10}
            {:schema account-name-age-sex-xm
             :model {:fill-empty {:account {:name (fn [env] "username")}}}})
  => {:db {:id 10}, :account {:name "username"}})

(fact "Fill with Refs"
  (t/normalise {:account/orders #{{:number 1}
                               {:number 2}}}
              {:schema account-orders-items-image-xm
               :model {:fill-empty {:account {:orders {:age 10}}}
                       :ignore {:account {:orders {:age :checked}}}}})
  => {:account {:orders #{{:number 2, :age 10}
                          {:number 1, :age 10}}}}

  (t/normalise {:account/orders #{{:number 1}
                               {:number 2}}}
              {:schema account-orders-items-image-xm
               :model {:fill-empty
                       {:account {:age 10
                                  :orders {:age (fn [refs]
                                                  (-> refs first :account :age))}}}
                       :ignore {:account {:age :checked
                                          :orders {:age :checked}}}}})
  => {:account {:age 10
                :orders #{{:number 2, :age 10}
                          {:number 1, :age 10}}}})
