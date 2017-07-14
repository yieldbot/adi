(ns spirit.pipeline.require-test
  (:use hara.test)
  (:require [data.examples :as examples]
            [spirit.schema :as schema]
            [spirit.pipeline :as pipeline]
            [spirit.pipeline.require :as require]))

(def ^:dynamic *wrappers*
  {:normalise        [pipeline/wrap-plus require/wrap-model-pre-require]
   :normalise-branch [pipeline/wrap-key-path]
   :normalise-attr   [pipeline/wrap-key-path]})

^{:refer spirit.pipeline.require/process-require :added "0.3"}
(fact "Used by both wrap-model-pre-require and wrap-model-post-require
  for determining correct input"
  (pipeline/normalise {:account/name "Chris"}
                       {:schema (schema/schema examples/account-name-age-sex)
                        :pipeline {:pre-require {:account {:name :checked}}}}
                       *wrappers*)
  => {:account {:name "Chris"}}

  (pipeline/normalise {:account/age 10}
                       {:schema (schema/schema examples/account-name-age-sex)
                        :pipeline {:pre-require {:account {:name :checked}}}}
                       *wrappers*)
  => (throws-info {:nsv [:account :name]
                    :no-required true}))

^{:refer spirit.pipeline.require/wrap-model-pre-require :added "0.3"}
(fact "require also works across refs"
  (pipeline/normalise {:account/orders #{{:number 1}
                                          {:number 2}}}
              {:schema (schema/schema examples/account-orders-items-image)
               :pipeline {:pre-require {:account {:orders {:number :checked}}}}}
              *wrappers*)
  => {:account {:orders #{{:number 1}
                          {:number 2}}}}
  (pipeline/normalise {:account/orders #{{:items {:name "stuff"}}
                                          {:number 2}}}
              {:schema (schema/schema examples/account-orders-items-image)
               :pipeline {:pre-require {:account {:orders {:number :checked}}}}}
              *wrappers*)
  => (throws-info {:data {:items {:name "stuff"}}
                    :nsv [:order :number]
                    :no-required true}))
