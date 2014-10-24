(ns adi.normalise.pipeline.convert-test
  (:use midje.sweet)
  (:require [adi.test.examples :as examples]
            [adi.test.checkers :refer :all]
            [adi.schema :as schema]
            [adi.normalise.base :as normalise]
            [adi.normalise.common.paths :as paths]
            [adi.normalise.pipeline.convert :as convert]))

(def ^:dynamic *wrappers*
  {:normalise        [paths/wrap-plus]
   :normalise-single [convert/wrap-single-model-convert]
   :normalise-branch [paths/wrap-key-path]
   :normalise-attr   [paths/wrap-key-path]})

^{:refer adi.normalise.pipeline.convert/wrap-single-model-convert :added "0.3"}
(fact "converts input according to model"
 (normalise/normalise {:account/name "Chris"}
                     {:schema (schema/schema examples/account-name-age-sex)
                      :model {:convert {:account {:name #(.toLowerCase %)}}}}
                     *wrappers*)
  => {:account {:name "chris"}})
