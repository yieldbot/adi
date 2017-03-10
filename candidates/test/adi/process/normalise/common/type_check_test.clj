(ns adi.process.normalise.common.type-check-test
  (:use midje.sweet)
  (:require [adi.process.normalise.base :as normalise]
            [adi.process.normalise.common.type-check :refer :all]
            [adi.schema :as schema]
            [adi.test.examples :as examples]
            [adi.test.checkers :refer [raises-issue]]))

^{:refer adi.process.normalise.common.type-check/wrap-single-type-check :added "0.3"}
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
