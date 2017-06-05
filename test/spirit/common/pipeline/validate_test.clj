(ns spirit.common.pipeline.validate-test
  (:use hara.test)
  (:require [data.examples :as examples]
            [spirit.common.schema :as schema]
            [spirit.common.pipeline :as pipeline]
            [spirit.common.pipeline.validate :as validate]))

(def ^:dynamic *wrappers*
  {:normalise        [pipeline/wrap-plus]
   :normalise-single [validate/wrap-single-model-validate]
   :normalise-branch [pipeline/wrap-key-path]
   :normalise-attr   [pipeline/wrap-key-path]})

^{:refer spirit.common.pipeline.validate/wrap-single-model-validate :added "0.3"}
(fact "validates input according to model"

 (pipeline/normalise {:account/name "Chris"}
                     {:schema (schema/schema examples/account-name-age-sex)
                      :pipeline {:validate {:account {:name number?}}}}
                     *wrappers*)
  => (throws-info {:not-validated true :nsv [:account :name]})

  (pipeline/normalise {:account/name "Bob"}
                       {:schema (schema/schema examples/account-name-age-sex)
                        :pipeline {:validate {:account {:name #(= % "Bob")}}}}
                       *wrappers*)
  => {:account {:name "Bob"}})
