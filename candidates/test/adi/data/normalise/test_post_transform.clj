(ns adi.data.normalise.test-post-transform
  (:use midje.sweet)
  (:require
   [adi-test.schemas :refer :all]
   [adi-test.checkers :refer :all]
   [adi.model :refer [model-input]]
   [adi.data.normalise :as t :refer [normalise]]))

(fact "Simple Post-Transform"
 (t/normalise {:account/name "Chris"}
           {:schema account-name-age-sex-xm
            :model {:post-transform {:account {:name "Bob"}}}})
 => {:account {:name "Bob"}}

 (t/normalise {:account/name "Chris"}
           {:schema account-name-age-sex-xm
            :model {:post-transform {:account {:name (fn [_ env] (:name env))}}}
            :name "Bob"})
 => {:account {:name "Bob"}}

 (t/normalise {:account/name "Chris"}
           {:schema account-name-age-sex-xm
            :model {:post-transform {:account {:name (fn [v _] (keyword v))}}}})
 => {:account {:name :Chris}}

 (t/normalise {:account/name "Chris"}
           {:schema account-name-age-sex-xm
            :model {:pre-mask {:account {:name :checked}}
                    :post-transform {:account {:name (fn [v _] (keyword v))}}}})
 => {:account {}}

 (t/normalise {:account/name "Chris"}
           {:schema account-name-age-sex-xm
            :model {:post-transform {:account (fn [_ env] {:name "Bob"})}}})
 => {:account {:name "Bob"}})

(fact "Post-Transform with Refs"
 (t/normalise {:account/orders #{{:+/db/id 1 :number 1}
                              {:+/db/id 2 :number 2}
                              {:+/db/id 3 :number 3}}}
             {:schema account-orders-items-image-xm
              :model {:post-transform {:account {:orders {:number 10}}}}})
 => {:account {:orders #{{:+ {:db {:id 3}}, :number 10}
                         {:+ {:db {:id 2}}, :number 10}
                         {:+ {:db {:id 1}}, :number 10}}}})
                         
