(ns adi.normalise.pipeline.allow-test
  (:use midje.sweet)
  (:require [adi.test.examples :as examples]
            [adi.test.checkers :refer :all]
            [adi.schema :as schema]
            [adi.normalise.base :as normalise]
            [adi.normalise.common.paths :as paths]
            [adi.normalise.pipeline.allow :as allow]))

(def ^:dynamic *wrappers*
  {:normalise        [paths/wrap-plus]
   :normalise-branch [allow/wrap-branch-model-allow paths/wrap-key-path]
   :normalise-attr   [allow/wrap-attr-model-allow paths/wrap-key-path]})

^{:refer adi.normalise.pipeline.allow/wrap-branch-model-allow :added "0.3"}
(fact "Works together with wrap-attr-model-allow to control access to data"
  (normalise/normalise {:account/name "Chris"}
                       {:schema (schema/schema examples/account-name-age-sex)
                        :model {:allow {}}}
                       *wrappers*)
  => (raises-issue {:adi true
                    :data {:name "Chris"}
                    :key-path [:account]
                    :normalise true
                    :not-allowed true
                    :nsv [:account]})

  (normalise/normalise {:account/name "Chris"}
                       {:schema (schema/schema examples/account-name-age-sex)
                        :model {:allow {:account {:name :checked}}}}
                       *wrappers*)
  => {:account {:name "Chris"}}
  ^:hidden
  (normalise/normalise {:account {:name "Chris"
                                  :age 10}}
                       {:schema (schema/schema examples/account-name-age-sex)
                        :model {:allow {:account {:name :checked}}}}
                       *wrappers*)
  => (raises-issue {:adi true
                    :data 10
                    :key-path [:account :age]
                    :normalise true
                    :not-allowed true
                    :nsv [:account :age]}))

^{:refer adi.normalise.pipeline.allow/wrap-branch-model-allow-refs :added "0.3"}
(fact "Allow with Refs"
  (normalise/normalise {:account/orders {:number 1}}
                       {:schema (schema/schema examples/account-orders-items-image)
                        :model {:allow {:account {:orders {:number :checked}}}}}
                       *wrappers*)
  => {:account {:orders {:number 1}}}

  (normalise/normalise {:account {:user "Chris"}}
                       {:schema (schema/schema examples/account-orders-items-image)
                        :model {:allow {:account {:user :checked}}}}
                       *wrappers*)
  => {:account {:user "Chris"}}


  (normalise/normalise {:account {:orders {:+ {:account {:user "Chris"}}}}}
                       {:schema (schema/schema examples/account-orders-items-image)
                        :model {:allow {:account {:user :checked
                                                  :orders {:+ {:account {:user :checked}}}}}}}
                       *wrappers*)
  => {:account {:orders {:+ {:account {:user "Chris"}}}}}


  (normalise/normalise {:account {:orders {:+ {:account {:user "Chris"}}}}}
                       {:schema (schema/schema examples/account-orders-items-image)
                        :model {:allow {:account {:user :checked
                                                  :orders {:+ {:order {:number :checked}}}}}}}
                       *wrappers*)
  => (raises-issue {:adi true
                    :key-path [:account :orders :+ :account]
                    :nsv [:account]
                    :data {:user "Chris"}
                    :not-allowed true
                    :normalise true}))
