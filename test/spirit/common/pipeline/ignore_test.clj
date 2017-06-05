(ns spirit.common.pipeline.ignore-test
  (:use hara.test)
  (:require [spirit.common.pipeline :as pipeline]
            [spirit.common.pipeline.ignore :as ignore]
            [spirit.common.schema :as schema]
            [data.examples :as examples]))

^{:refer spirit.common.pipeline.ignore/wrap-nil-model-ignore :added "0.3"}
(fact "wraps the normalise-nil function such that any unknown keys are ignored"
  (pipeline/normalise {:account {:name "Chris"
                       :age 10
                       :parents ["henry" "sally"]}}
               {:schema (schema/schema examples/account-name-age-sex)
                :pipeline {:ignore {:account {:parents :checked}}}}
               {:normalise-nil [ignore/wrap-nil-model-ignore]})
  => {:account {:name "Chris"
                :age 10
                :parents ["henry" "sally"]}}
  ^:hidden
  (pipeline/normalise {:account {:name "Chris"
                       :age 10
                       :parents ["henry" "sally"]}}
             {:schema (schema/schema examples/account-name-age-sex)}
             {:normalise-branch [pipeline/wrap-key-path]})
  => (throws-info {:key-path [:account]
                    :normalise true
                    :nsv [:account :parents]
                    :no-schema true}))
