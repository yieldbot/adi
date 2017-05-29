(ns spirit.process.normalise.common.type-check-test
  (:use hara.test)
  (:require [spirit.process.normalise.base :as normalise]
            [spirit.process.normalise.common.type-check :refer :all]
            [spirit.schema :as schema]
            [spirit.test.examples :as examples]
            [spirit.test.checkers :refer [raises-issue]]))

^{:refer spirit.process.normalise.common.type-check/wrap-single-type-check :added "0.3"}
(fact "wraps normalise to type check inputs as well as to coerce incorrect inputs"
  (normalise/normalise {:account {:age "10"}}
                       {:schema (schema/schema examples/account-name-age-sex)}
                       {:normalise-single [wrap-single-type-check]})
  => (raises-issue {:type :long,
                    :data "10",
                    :wrong-type true})

  (normalise/normalise {:account {:age "10"}}
                       {:schema (schema/schema examples/account-name-age-sex)
                        :options {:use-coerce true}}
                       {:normalise-single [wrap-single-type-check]})
  => {:account {:age 10}})
