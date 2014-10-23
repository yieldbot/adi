(ns adi.normalise.pipeline.ignore-test
  (:use midje.sweet)
  (:require [adi.normalise.base :as normalise]
            [adi.normalise.pipeline.ignore :refer :all]
            [adi.schema :as schema]
            [adi.test.examples :as examples]
            [adi.test.checkers :refer [raises-issue]]))

^{:refer adi.normalise.pipeline.ignore/wrap-nil-model-ignore :added "0.3"}
(fact "Simple Ignore"
  (normalise/normalise {:account {:name "Chris"
                       :age 10
                       :parents ["henry" "sally"]}}
             {:schema (schema/schema examples/account-name-age-sex)})
  => (raises-issue {:key-path [:account],
                    :normalise true,
                    :nsv [:account :parents],
                    :no-schema true})

  (normalise/normalise {:account {:name "Chris"
                       :age 10
                       :parents ["henry" "sally"]}}
               {:schema (schema/schema examples/account-name-age-sex)
                :model {:ignore {:account {:parents :checked}}}}
               {:normalise-nil [wrap-nil-model-ignore]})
  => {:account {:name "Chris"
                :age 10
                :parents ["henry" "sally"]}})
