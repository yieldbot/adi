(ns spirit.datomic.process.normalise.pipeline.validate-test
  (:use hara.test)
  (:require [data.examples :as examples]
            [spirit.common.schema :as schema]
            [spirit.datomic.process.normalise.base :as normalise]
            [spirit.datomic.process.normalise.common.paths :as paths]
            [spirit.datomic.process.normalise.pipeline.validate :as validate]))

(def ^:dynamic *wrappers*
  {:normalise        [paths/wrap-plus]
   :normalise-single [validate/wrap-single-model-validate]
   :normalise-branch [paths/wrap-key-path]
   :normalise-attr   [paths/wrap-key-path]})

^{:refer spirit.datomic.process.normalise.pipeline.validate/wrap-single-model-validate :added "0.3"}
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
