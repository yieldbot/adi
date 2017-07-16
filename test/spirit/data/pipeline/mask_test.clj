(ns spirit.data.pipeline.mask-test
  (:use hara.test)
  (:require [data.examples :as examples]
            [spirit.data.schema :as schema]
            [spirit.data.pipeline :as pipeline]
            [spirit.data.pipeline.mask :as mask]))

(def ^:dynamic *wrappers*
  {:normalise        [pipeline/wrap-plus mask/wrap-model-pre-mask]
   :normalise-branch [pipeline/wrap-key-path]
   :normalise-attr   [pipeline/wrap-key-path]})

^{:refer spirit.data.pipeline.mask/process-mask :added "0.3"}
(fact "Used by both wrap-model-pre-mask and wrap-model-post-mask
  for determining correct input"
  (pipeline/normalise {:account/name "Chris"}
                       {:schema (schema/schema examples/account-name-age-sex)
                        :pipeline {:pre-mask {:account {:name :checked}}}}
                       *wrappers*)
  => {:account {}}

  (pipeline/normalise {:account/age 10}
                       {:schema (schema/schema examples/account-name-age-sex)
                        :pipeline {:pre-mask {:account :checked}}}
                       *wrappers*)
  => {}
  ^:hidden
  (pipeline/normalise {:account/age 10}
                       {:schema (schema/schema examples/account-name-age-sex)
                        :pipeline {:pre-mask {:account {:name :checked}}}}
                       *wrappers*)
  => {:account {:age 10}})

^{:refer spirit.data.pipeline.mask/wrap-model-pre-mask :added "0.3"}
(fact "mask also works across refs"
  (pipeline/normalise {:account/orders #{{:number 1 :items {:name "one"}}
                                          {:number 2 :items {:name "two"}}}}
              {:schema (schema/schema examples/account-orders-items-image)
               :pipeline {:pre-mask {:account {:orders {:number :checked}}}}}
              *wrappers*)
  => {:account {:orders #{{:items {:name "one"}}
                          {:items {:name "two"}}}}})
