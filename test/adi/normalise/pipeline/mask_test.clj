(ns adi.normalise.pipeline.mask-test
  (:use midje.sweet)
  (:require [adi.test.examples :as examples]
            [adi.test.checkers :refer :all]
            [adi.schema :as schema]
            [adi.normalise.base :as normalise]
            [adi.normalise.common.paths :as paths]
            [adi.normalise.pipeline.mask :as mask]))

(def ^:dynamic *wrappers*
  {:normalise        [paths/wrap-plus mask/wrap-model-pre-mask]
   :normalise-branch [paths/wrap-key-path]
   :normalise-attr   [paths/wrap-key-path]})

^{:refer adi.normalise.pipeline.mask/process-mask :added "0.3"}
(fact "Used by both wrap-model-pre-mask and wrap-model-post-mask
  for determining correct input"
  (normalise/normalise {:account/name "Chris"}
                       {:schema (schema/schema examples/account-name-age-sex)
                        :model {:pre-mask {:account {:name :checked}}}}
                       *wrappers*)
  => {:account {}}

  (normalise/normalise {:account/age 10}
                       {:schema (schema/schema examples/account-name-age-sex)
                        :model {:pre-mask {:account :checked}}}
                       *wrappers*)
  => {}
  ^:hidden
  (normalise/normalise {:account/age 10}
                       {:schema (schema/schema examples/account-name-age-sex)
                        :model {:pre-mask {:account {:name :checked}}}}
                       *wrappers*)
  => {:account {:age 10}})

^{:refer adi.normalise.pipeline.mask/wrap-model-pre-mask :added "0.3"}
(fact "mask also works across refs"
  (normalise/normalise {:account/orders #{{:number 1 :items {:name "one"}}
                                          {:number 2 :items {:name "two"}}}}
              {:schema (schema/schema examples/account-orders-items-image)
               :model {:pre-mask {:account {:orders {:number :checked}}}}}
              *wrappers*)
  => {:account {:orders #{{:items {:name "one"}}
                          {:items {:name "two"}}}}})
