(ns spirit.common.normalise.validate-test
  (:use hara.test)
  (:require [data.examples :as examples]
            [spirit.common.schema :as schema]
            [spirit.common.normalise :as normalise]
            [spirit.common.normalise.validate :as validate]))

(def ^:dynamic *wrappers*
  {:normalise        [normalise/wrap-plus]
   :normalise-single [validate/wrap-single-model-validate]
   :normalise-branch [normalise/wrap-key-path]
   :normalise-attr   [normalise/wrap-key-path]})

^{:refer spirit.common.normalise.validate/wrap-single-model-validate :added "0.3"}
(fact "validates input according to model"

 (normalise/normalise {:account/name "Chris"}
                     {:schema (schema/schema examples/account-name-age-sex)
                      :pipeline {:validate {:account {:name number?}}}}
                     *wrappers*)
  => (throws-info {:not-validated true :nsv [:account :name]})

  (normalise/normalise {:account/name "Bob"}
                       {:schema (schema/schema examples/account-name-age-sex)
                        :pipeline {:validate {:account {:name #(= % "Bob")}}}}
                       *wrappers*)
  => {:account {:name "Bob"}})
