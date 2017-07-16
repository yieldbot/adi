(ns spirit.data.pipeline.convert-test
  (:use hara.test)
  (:require [data.examples :as examples]
            [spirit.data.schema :as schema]
            [spirit.data.pipeline :as pipeline]
            [spirit.data.pipeline.convert :as convert]))

(def ^:dynamic *wrappers*
  {:normalise        [pipeline/wrap-plus]
   :normalise-single [convert/wrap-single-model-convert]
   :normalise-branch [pipeline/wrap-key-path]
   :normalise-attr   [pipeline/wrap-key-path]})

^{:refer spirit.data.pipeline.convert/wrap-single-model-convert :added "0.3"}
(fact "converts input according to model"
 (pipeline/normalise {:account/name "Chris"}
                     {:schema (schema/schema examples/account-name-age-sex)
                      :pipeline {:convert {:account {:name #(.toLowerCase %)}}}}
                     *wrappers*)
  => {:account {:name "chris"}})
