(ns adi.process.normalise.pipeline.fill-assoc-test
  (:use midje.sweet)
  (:require [adi.process.normalise.base :as normalise]
            [adi.process.normalise.pipeline.fill-assoc :as fill]
            [adi.process.normalise.pipeline.ignore :as ignore]
            [adi.process.normalise.common.paths :as paths]
            [adi.process.normalise.common.db :as db]
            [adi.schema :as schema]
            [adi.test.examples :as examples]
            [adi.test.checkers :refer [raises-issue]]))

(def ^:dynamic *wrappers*
  {:normalise        [db/wrap-db paths/wrap-plus fill/wrap-model-fill-assoc]
   :normalise-nil    [ignore/wrap-nil-model-ignore]
   :normalise-branch [paths/wrap-key-path]
   :normalise-attr   [paths/wrap-key-path]})
   
^{:refer adi.process.normalise.pipeline.fill-assoc/wrap-model-fill-assoc :added "0.3"}
(fact "fills data by associating additional elements"
  (normalise/normalise {:account/name "Chris" :account/age 9}
            {:schema (schema/schema examples/account-name-age-sex)
             :model {:fill-assoc {:account {:age 10}}}}
            *wrappers*)
  => {:account {:name "Chris", :age #{9 10}}}

  (normalise/normalise {:account/name "Chris"}
            {:schema (schema/schema examples/account-name-age-sex)
             :model {:fill-assoc {:account {:age (fn [_ env]
                                                   (:age env))}}}
             :age 10}
            *wrappers*)
  => {:account {:name "Chris", :age 10}}
  ^:hidden
  (normalise/normalise {:db/id 10}
            {:schema (schema/schema examples/account-name-age-sex)
             :model {:fill-assoc {:account {:name (fn [env] "username")}}}}
             *wrappers*)
  => {:db {:id 10}, :account {:name "username"}}

  (normalise/normalise {:account/orders #{{:number 1}
                                  {:number 2}}}
              {:schema (schema/schema examples/account-orders-items-image)
               :model {:fill-assoc {:account {:orders (fn [] {:number 4})}}}}
            *wrappers*)
  => {:account {:orders #{{:number 2} {:number 1} {:number 4}}}}

  (normalise/normalise {:account/orders #{{:number 1 :age 11}
                               {:number 2  :age 11}}}
              {:schema (schema/schema examples/account-orders-items-image)
               :model {:fill-assoc
                       {:account {:age 10
                                  :orders {:age (fn [refs]
                                                  (-> refs first :account :age))}}}
                       :ignore {:account {:age :checked
                                          :orders {:age :checked}}}}}
              *wrappers*)
  => {:account {:age 10
                :orders #{{:number 2, :age #{11 10}}
                          {:number 1, :age #{11 10}}}}})
