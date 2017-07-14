(ns spirit.pipeline.allow-test
  (:use hara.test)
  (:require [data.examples :as examples]
            [spirit.schema :as schema]
            [spirit.pipeline :as pipeline]
            [spirit.pipeline.allow :as allow]))

(def ^:dynamic *wrappers*
  {:normalise        [pipeline/wrap-plus]
   :normalise-branch [allow/wrap-branch-model-allow pipeline/wrap-key-path]
   :normalise-attr   [allow/wrap-attr-model-allow pipeline/wrap-key-path]})

^{:refer spirit.pipeline.allow/wrap-branch-model-allow :added "0.3"}
(fact "Works together with wrap-attr-model-allow to control access to data"
  (pipeline/normalise {:account/name "Chris"}
                       {:schema (schema/schema examples/account-name-age-sex)
                        :pipeline {:allow {}}}
                       *wrappers*)
  => (throws-info {:data {:name "Chris"}
                   :key-path [:account]
                   :normalise true
                   :not-allowed true
                   :nsv [:account]})
  
  (pipeline/normalise {:account/name "Chris"}
                       {:schema (schema/schema examples/account-name-age-sex)
                        :pipeline {:allow {:account {:name :checked}}}}
                       *wrappers*)
  => {:account {:name "Chris"}}
  ^:hidden
  (pipeline/normalise {:account {:name "Chris"
                                  :age 10}}
                       {:schema (schema/schema examples/account-name-age-sex)
                        :pipeline {:allow {:account {:name :checked}}}}
                       *wrappers*)
  => (throws-info {:data 10
                   :key-path [:account :age]
                   :normalise true
                   :not-allowed true
                   :nsv [:account :age]}))

^{:refer spirit.pipeline.allow/wrap-branch-model-allow-refs :added "0.3"}
(fact "Allow with Refs"
  (pipeline/normalise {:account/orders {:number 1}}
                       {:schema (schema/schema examples/account-orders-items-image)
                        :pipeline {:allow {:account {:orders {:number :checked}}}}}
                       *wrappers*)
  => {:account {:orders {:number 1}}}

  (pipeline/normalise {:account {:user "Chris"}}
                       {:schema (schema/schema examples/account-orders-items-image)
                        :pipeline {:allow {:account {:user :checked}}}}
                       *wrappers*)
  => {:account {:user "Chris"}}


  (pipeline/normalise {:account {:orders {:+ {:account {:user "Chris"}}}}}
                       {:schema (schema/schema examples/account-orders-items-image)
                        :pipeline {:allow {:account {:user :checked
                                                  :orders {:+ {:account {:user :checked}}}}}}}
                       *wrappers*)
  => {:account {:orders {:+ {:account {:user "Chris"}}}}}


  (pipeline/normalise {:account {:orders {:+ {:account {:user "Chris"}}}}}
                       {:schema (schema/schema examples/account-orders-items-image)
                        :pipeline {:allow {:account {:user :checked
                                                  :orders {:+ {:order {:number :checked}}}}}}}
                       *wrappers*)
  => (throws-info {:key-path [:account :orders :+ :account]
                   :nsv [:account]
                   :data {:user "Chris"}
                   :not-allowed true
                   :normalise true}))
