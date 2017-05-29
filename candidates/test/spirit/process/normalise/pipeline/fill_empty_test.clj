(ns spirit.process.normalise.pipeline.fill-empty-test
  (:use hara.test)
  (:require [spirit.process.normalise.base :as normalise]
            [spirit.process.normalise.pipeline.fill-empty :as fill]
            [spirit.process.normalise.pipeline.ignore :as ignore]
            [spirit.process.normalise.common.paths :as paths]
            [spirit.process.normalise.common.db :as db]
            [spirit.schema :as schema]
            [spirit.test.examples :as examples]
            [spirit.test.checkers :refer [raises-issue]]))

(def ^:dynamic *wrappers*
  {:normalise        [db/wrap-db paths/wrap-plus fill/wrap-model-fill-empty]
   :normalise-nil    [ignore/wrap-nil-model-ignore]
   :normalise-branch [paths/wrap-key-path]
   :normalise-attr   [paths/wrap-key-path]})
   
^{:refer spirit.process.normalise.pipeline.fill-empty/wrap-model-fill-empty :added "0.3"}
(fact "fills data by associating additional elements"
  (normalise/normalise {:account/name "Chris" :account/age 9}
            {:schema (schema/schema examples/account-name-age-sex)
             :pipeline {:fill-empty {:account {:age 10}}}}
            *wrappers*)
  => {:account {:name "Chris", :age 9}}

  (normalise/normalise {:account/name "Chris"}
            {:schema (schema/schema examples/account-name-age-sex)
             :pipeline {:fill-empty {:account {:age (fn [_ env]
                                                   (:age env))}}}
             :age 10}
            *wrappers*)
  => {:account {:name "Chris", :age 10}}
  ^:hidden
  (normalise/normalise {:db/id 10}
            {:schema (schema/schema examples/account-name-age-sex)
             :pipeline {:fill-empty {:account {:name (fn [env] "username")}}}}
             *wrappers*)
  => {:db {:id 10}, :account {:name "username"}}

  #_(normalise/normalise {:account/orders #{{:number 1}
                                  {:number 2}}}
              {:schema (schema/schema examples/account-orders-items-image)
               :pipeline {:fill-empty {:account {:orders (fn [] {:number 4})}}}}
            *wrappers*)
  ;;=> {:account {:orders #{{:number 2} {:number 1} {:number 4}}}}

  (normalise/normalise {:account/orders #{{:number 1 :age 11}
                               {:number 2  :age 11}}}
              {:schema (schema/schema examples/account-orders-items-image)
               :pipeline {:fill-empty
                       {:account {:age 10
                                  :orders {:age (fn [refs]
                                                  (-> refs first :account :age))}}}
                       :ignore {:account {:age :checked
                                          :orders {:age :checked}}}}}
              *wrappers*)
  => {:account {:age 10
                :orders #{{:number 2, :age 11}
                          {:number 1, :age 11}}}})
