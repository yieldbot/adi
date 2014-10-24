(ns adi.normalise.pipeline.require-test
  (:use midje.sweet)
  (:require [adi.test.examples :as examples]
            [adi.test.checkers :refer :all]
            [adi.schema :as schema]
            [adi.normalise.base :as normalise]
            [adi.normalise.common.paths :as paths]
            [adi.normalise.pipeline.require :as require]))

(def ^:dynamic *wrappers*
  {:normalise        [paths/wrap-plus require/wrap-model-pre-require]
   :normalise-branch [paths/wrap-key-path]
   :normalise-attr   [paths/wrap-key-path]})

^{:refer adi.normalise.pipeline.require/process-require :added "0.3"}
(fact "Used by both wrap-model-pre-require and wrap-model-post-require
  for determining correct input"
  (normalise/normalise {:account/name "Chris"}
                       {:schema (schema/schema examples/account-name-age-sex)
                        :model {:pre-require {:account {:name :checked}}}}
                       *wrappers*)
  => {:account {:name "Chris"}}

  (normalise/normalise {:account/age 10}
                       {:schema (schema/schema examples/account-name-age-sex)
                        :model {:pre-require {:account {:name :checked}}}}
                       *wrappers*)
  => (raises-issue {:nsv [:account :name]
                    :no-required true}))

^{:refer adi.normalise.pipeline.require/wrap-model-pre-require :added "0.3"}
(fact "require also works across refs"
  (normalise/normalise {:account/orders #{{:number 1}
                                          {:number 2}}}
              {:schema (schema/schema examples/account-orders-items-image)
               :model {:pre-require {:account {:orders {:number :checked}}}}}
              *wrappers*)
  => {:account {:orders #{{:number 1}
                          {:number 2}}}}
  (normalise/normalise {:account/orders #{{:items {:name "stuff"}}
                                          {:number 2}}}
              {:schema (schema/schema examples/account-orders-items-image)
               :model {:pre-require {:account {:orders {:number :checked}}}}}
              *wrappers*)
  => (raises-issue {:data {:items {:name "stuff"}}
                    :nsv [:order :number]
                    :no-required true}))
