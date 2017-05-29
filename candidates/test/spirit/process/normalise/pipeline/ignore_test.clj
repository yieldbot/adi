(ns spirit.process.normalise.pipeline.ignore-test
  (:use hara.test)
  (:require [spirit.process.normalise.base :as normalise]
            [spirit.process.normalise.pipeline.ignore :as ignore]
            [spirit.process.normalise.common.paths :as paths]
            [spirit.schema :as schema]
            [spirit.test.examples :as examples]
            [spirit.test.checkers :refer [raises-issue]]))

^{:refer spirit.process.normalise.pipeline.ignore/wrap-nil-model-ignore :added "0.3"}
(fact "wraps the normalise-nil function such that any unknown keys are ignored"
  (normalise/normalise {:account {:name "Chris"
                       :age 10
                       :parents ["henry" "sally"]}}
               {:schema (schema/schema examples/account-name-age-sex)
                :pipeline {:ignore {:account {:parents :checked}}}}
               {:normalise-nil [ignore/wrap-nil-model-ignore]})
  => {:account {:name "Chris"
                :age 10
                :parents ["henry" "sally"]}}
  ^:hidden
  (normalise/normalise {:account {:name "Chris"
                       :age 10
                       :parents ["henry" "sally"]}}
             {:schema (schema/schema examples/account-name-age-sex)}
             {:normalise-branch [paths/wrap-key-path]})
  => (raises-issue {:key-path [:account]
                    :normalise true
                    :nsv [:account :parents]
                    :no-schema true}))