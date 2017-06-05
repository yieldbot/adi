(ns spirit.common.normalise.transform-test
  (:use hara.test)
  (:require [data.examples :as examples]
            [spirit.common.schema :as schema]
            [spirit.common.normalise :as normalise]
            [spirit.common.normalise.transform :as transform]))

(def ^:dynamic *wrappers*
  {:normalise        [normalise/wrap-plus transform/wrap-model-pre-transform]
   :normalise-branch [normalise/wrap-key-path]
   :normalise-attr   [normalise/wrap-key-path]})

^{:refer spirit.common.normalise.transform/process-transform :added "0.3"}
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

^{:refer spirit.common.normalise.transform/wrap-model-pre-transform :added "0.3"}
(fact "transform also works across refs"
  (normalise/normalise {:account/orders #{{:number 1 :items {:name "one"}}
                                          {:number 2 :items {:name "two"}}}}
              {:schema (schema/schema examples/account-orders-items-image)
               :pipeline {:pre-transform {:account {:orders {:number inc
                                                          :items {:name "thing"}}}}}}
              *wrappers*)
  => {:account {:orders #{{:items {:name "thing"}, :number 2}
                          {:items {:name "thing"}, :number 3}}}})
