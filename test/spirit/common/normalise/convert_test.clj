(ns spirit.common.normalise.convert-test
  (:use hara.test)
  (:require [data.examples :as examples]
            [spirit.common.schema :as schema]
            [spirit.common.normalise :as normalise]
            [spirit.common.normalise.convert :as convert]))

(def ^:dynamic *wrappers*
  {:normalise        [normalise/wrap-plus]
   :normalise-single [convert/wrap-single-model-convert]
   :normalise-branch [normalise/wrap-key-path]
   :normalise-attr   [normalise/wrap-key-path]})

^{:refer spirit.common.normalise.convert/wrap-single-model-convert :added "0.3"}
(fact "converts input according to model"
 (normalise/normalise {:account/name "Chris"}
                     {:schema (schema/schema examples/account-name-age-sex)
                      :pipeline {:convert {:account {:name #(.toLowerCase %)}}}}
                     *wrappers*)
  => {:account {:name "chris"}})
