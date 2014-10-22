(ns adi.data.normalise.test-post-require
  (:use midje.sweet)
  (:require
   [adi-test.schemas :refer :all]
   [adi-test.checkers :refer :all]
   [adi.model :refer [model-input]]
   [adi.data.normalise :as t :refer [normalise]]))

(fact "Simple Post Require"
  (t/normalise {:account/name "Chris"}
            {:schema account-name-age-sex-xm
             :model {:post-require {:account {:name :checked}}}})
  => {:account {:name "Chris"}}

  (t/normalise {:account/age 10}
            {:schema account-name-age-sex-xm
             :model {:post-require {:account {:name :checked}}}})
  => (raises-issue {:adi true :no-required true})

  (t/normalise {:account/name "Chris"}
            {:schema account-name-age-sex-xm
             :model {:pre-mask {:account {:name :checked}}
                     :post-require {:account {:name :checked}}}})
  => (raises-issue {:adi true :no-required true}))

(fact "Ref Post Require"
  (t/normalise {:account/orders #{{:number 1
                                   :items {:name "hello"}}
                                  {:number 2}}}
              {:schema account-orders-items-image-xm
               :model {:post-require {:account {:orders {:number :checked}}}}})
  => {:account {:orders #{{:number 1
                           :items #{{:name "hello"}}}
                          {:number 2}}}}

  (t/normalise {:account/orders #{{:items {:name "hello"}}
                                  {:number 2}}}
              {:schema account-orders-items-image-xm
               :model {:post-require {:account {:orders {:number :checked}}}}})
  => (raises-issue {:adi true :no-required true}))
