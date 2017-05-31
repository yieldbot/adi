(ns spirit.datomic.process.normalise.pipeline.transform-test
  (:use hara.test)
  (:require [data.examples :as examples]
            [spirit.common.schema :as schema]
            [spirit.datomic.process.normalise.base :as normalise]
            [spirit.datomic.process.normalise.common.paths :as paths]
            [spirit.datomic.process.normalise.pipeline.transform :as transform]))

(def ^:dynamic *wrappers*
  {:normalise        [paths/wrap-plus transform/wrap-model-pre-transform]
   :normalise-branch [paths/wrap-key-path]
   :normalise-attr   [paths/wrap-key-path]})

^{:refer spirit.datomic.process.normalise.pipeline.transform/process-transform :added "0.3"}
(fact "Used by both wrap-model-pre-transform and wrap-model-post-transform
  for determining correct input"

  (normalise/normalise {:account/name "Chris"}
                       {:schema (schema/schema examples/account-name-age-sex)
                        :pipeline {:pre-transform {:account {:name "Bob"}}}}
                       *wrappers*)
  => {:account {:name "Bob"}}

  (normalise/normalise {:account/name "Chris"}
                       {:schema (schema/schema examples/account-name-age-sex)
                        :name "Bob"
                        :pipeline {:pre-transform {:account {:name (fn [_ env] (:name env))}}}}
                       *wrappers*)
  => {:account {:name "Bob"}}

  (normalise/normalise {:account/name "Chris"}
                       {:schema (schema/schema examples/account-name-age-sex)
                        :name "Bob"
                        :pipeline {:pre-transform {:account {:name (fn [v] (str v "tian"))}}}}
                       *wrappers*)
  => {:account {:name "Christian"}})

^{:refer spirit.datomic.process.normalise.pipeline.transform/wrap-model-pre-transform :added "0.3"}
(fact "transform also works across refs"
  (normalise/normalise {:account/orders #{{:number 1 :items {:name "one"}}
                                          {:number 2 :items {:name "two"}}}}
              {:schema (schema/schema examples/account-orders-items-image)
               :pipeline {:pre-transform {:account {:orders {:number inc
                                                          :items {:name "thing"}}}}}}
              *wrappers*)
  => {:account {:orders #{{:items {:name "thing"}, :number 2}
                          {:items {:name "thing"}, :number 3}}}})
