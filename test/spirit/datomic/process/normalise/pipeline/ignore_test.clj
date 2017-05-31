(ns spirit.datomic.process.normalise.pipeline.ignore-test
  (:use hara.test)
  (:require [spirit.datomic.process.normalise.base :as normalise]
            [spirit.datomic.process.normalise.pipeline.ignore :as ignore]
            [spirit.datomic.process.normalise.common.paths :as paths]
            [spirit.common.schema :as schema]
            [data.examples :as examples]
            ))

^{:refer spirit.datomic.process.normalise.pipeline.ignore/wrap-nil-model-ignore :added "0.3"}
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
  => (throws-info {:key-path [:account]
                    :normalise true
                    :nsv [:account :parents]
                    :no-schema true}))