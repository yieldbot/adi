(ns adi.data.normalise.test-fill-assoc
  (:use midje.sweet)
  (:require
   [adi-test.schemas :refer :all]
   [adi-test.checkers :refer :all]
   [adi.model :refer [model-input]]
   [adi.data.normalise :as t :refer [normalise]]))

(fact "Simple Fill"
  (t/normalise {:account/name "Chris"}
            {:schema account-name-age-sex-xm
             :model {:fill-assoc {:account {:age 10}}}})
  => {:account {:name "Chris", :age 10}}

  (t/normalise {:account/name "Chris"}
            {:schema account-name-age-sex-xm
             :model {:fill-assoc {:account {:age (fn [refs env]
                                                   (:age env))}}}
             :age 10})
  => {:account {:name "Chris", :age 10}}

  (t/normalise {:db/id 10}
            {:schema account-name-age-sex-xm
             :model {:fill-assoc {:account {:name (fn [env] "username")}}}})
  => {:db {:id 10}, :account {:name "username"}})


(fact "Assocs"
  (t/normalise {:account/orders #{{:number 1}
                                  {:number 2}}}
              {:schema account-orders-items-image-xm
               :model {:fill-assoc {:account {:orders (fn [_] {:number 4})}}}})
  => {:account {:orders #{{:number 2} {:number 1} {:number 4}}}})

(fact "Fill with Refs"
  (t/normalise {:account/orders #{{:number 1 :age 11}
                               {:number 2  :age 11}}}
              {:schema account-orders-items-image-xm
               :model {:fill-assoc
                       {:account {:age 10
                                  :orders {:age (fn [refs]
                                                  (-> refs first :account :age))}}}
                       :ignore {:account {:age :checked
                                          :orders {:age :checked}}}}})
  => {:account {:age 10
                :orders #{{:number 2, :age #{11 10}}
                          {:number 1, :age #{11 10}}}}})
