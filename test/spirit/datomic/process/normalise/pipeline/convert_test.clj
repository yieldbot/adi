(ns spirit.datomic.process.normalise.pipeline.convert-test
  (:use hara.test)
  (:require [data.examples :as examples]
            [spirit.common.schema :as schema]
            [spirit.datomic.process.normalise.base :as normalise]
            [spirit.datomic.process.normalise.common.paths :as paths]
            [spirit.datomic.process.normalise.pipeline.convert :as convert]))

(def ^:dynamic *wrappers*
  {:normalise        [paths/wrap-plus]
   :normalise-single [convert/wrap-single-model-convert]
   :normalise-branch [paths/wrap-key-path]
   :normalise-attr   [paths/wrap-key-path]})

^{:refer spirit.datomic.process.normalise.pipeline.convert/wrap-single-model-convert :added "0.3"}
(fact "converts input according to model"
 (normalise/normalise {:account/name "Chris"}
                     {:schema (schema/schema examples/account-name-age-sex)
                      :pipeline {:convert {:account {:name #(.toLowerCase %)}}}}
                     *wrappers*)
  => {:account {:name "chris"}})
