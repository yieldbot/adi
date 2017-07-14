(ns spirit.pipeline.convert-test
  (:use hara.test)
  (:require [data.examples :as examples]
            [spirit.schema :as schema]
            [spirit.pipeline :as pipeline]
            [spirit.pipeline.convert :as convert]))

(def ^:dynamic *wrappers*
  {:normalise        [pipeline/wrap-plus]
   :normalise-single [convert/wrap-single-model-convert]
   :normalise-branch [pipeline/wrap-key-path]
   :normalise-attr   [pipeline/wrap-key-path]})

^{:refer spirit.pipeline.convert/wrap-single-model-convert :added "0.3"}
(fact "converts input according to model"
 (pipeline/normalise {:account/name "Chris"}
                     {:schema (schema/schema examples/account-name-age-sex)
                      :pipeline {:convert {:account {:name #(.toLowerCase %)}}}}
                     *wrappers*)
  => {:account {:name "chris"}})
