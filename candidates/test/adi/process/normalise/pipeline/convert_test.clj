(ns adi.process.normalise.pipeline.convert-test
  (:use midje.sweet)
  (:require [adi.test.examples :as examples]
            [adi.test.checkers :refer :all]
            [adi.schema :as schema]
            [adi.process.normalise.base :as normalise]
            [adi.process.normalise.common.paths :as paths]
            [adi.process.normalise.pipeline.convert :as convert]))

(def ^:dynamic *wrappers*
  {:normalise        [paths/wrap-plus]
   :normalise-single [convert/wrap-single-model-convert]
   :normalise-branch [paths/wrap-key-path]
   :normalise-attr   [paths/wrap-key-path]})

^{:refer adi.process.normalise.pipeline.convert/wrap-single-model-convert :added "0.3"}
(fact "converts input according to model"
 (normalise/normalise {:account/name "Chris"}
                     {:schema (schema/schema examples/account-name-age-sex)
                      :pipeline {:convert {:account {:name #(.toLowerCase %)}}}}
                     *wrappers*)
  => {:account {:name "chris"}})
