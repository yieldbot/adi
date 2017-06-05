(ns spirit.common.normalise.mask-test
  (:use hara.test)
  (:require [data.examples :as examples]
            [spirit.common.schema :as schema]
            [spirit.common.normalise :as normalise]
            [spirit.common.normalise.mask :as mask]))

(def ^:dynamic *wrappers*
  {:normalise        [normalise/wrap-plus mask/wrap-model-pre-mask]
   :normalise-branch [normalise/wrap-key-path]
   :normalise-attr   [normalise/wrap-key-path]})

^{:refer spirit.common.normalise.mask/process-mask :added "0.3"}
(fact "Used by both wrap-model-pre-mask and wrap-model-post-mask
  for determining correct input"
  (normalise/normalise {:account/name "Chris"}
                       {:schema (schema/schema examples/account-name-age-sex)
                        :pipeline {:pre-mask {:account {:name :checked}}}}
                       *wrappers*)
  => {:account {}}

  (normalise/normalise {:account/age 10}
                       {:schema (schema/schema examples/account-name-age-sex)
                        :pipeline {:pre-mask {:account :checked}}}
                       *wrappers*)
  => {}
  ^:hidden
  (normalise/normalise {:account/age 10}
                       {:schema (schema/schema examples/account-name-age-sex)
                        :pipeline {:pre-mask {:account {:name :checked}}}}
                       *wrappers*)
  => {:account {:age 10}})

^{:refer spirit.common.normalise.mask/wrap-model-pre-mask :added "0.3"}
(fact "mask also works across refs"
  (normalise/normalise {:account/orders #{{:number 1 :items {:name "one"}}
                                          {:number 2 :items {:name "two"}}}}
              {:schema (schema/schema examples/account-orders-items-image)
               :pipeline {:pre-mask {:account {:orders {:number :checked}}}}}
              *wrappers*)
  => {:account {:orders #{{:items {:name "one"}}
                          {:items {:name "two"}}}}})
