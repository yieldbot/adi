(ns spirit.data.pipeline.validate-test
  (:use hara.test)
  (:require [data.examples :as examples]
            [spirit.data.schema :as schema]
            [spirit.data.pipeline :as pipeline]
            [spirit.data.pipeline.validate :as validate]))

(def ^:dynamic *wrappers*
  {:normalise        [pipeline/wrap-plus]
   :normalise-single [validate/wrap-single-model-validate]
   :normalise-branch [pipeline/wrap-key-path]
   :normalise-attr   [pipeline/wrap-key-path]})

^{:refer spirit.data.pipeline.validate/wrap-single-model-validate :added "0.3"}
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
