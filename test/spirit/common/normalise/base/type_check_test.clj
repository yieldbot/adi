(ns spirit.common.normalise.base.type-check-test
  (:use hara.test)
  (:require [spirit.common.normalise :as normalise]
            [spirit.common.normalise.base.type-check :refer :all]
            [spirit.common.schema :as schema]
            [data.examples :as examples]))

^{:refer spirit.common.normalise.base.type-check/wrap-single-type-check :added "0.3"}
(fact "wraps normalise to type check inputs as well as to coerce incorrect inputs"
  (normalise/normalise {:account {:age "10"}}
                       {:schema (schema/schema examples/account-name-age-sex)}
                       {:normalise-single [wrap-single-type-check]})
  => (throws-info {:type :long,
                   :data "10",
                   :wrong-type true})
  
  (normalise/normalise {:account {:age "10"}}
                       {:schema (schema/schema examples/account-name-age-sex)
                        :options {:use-coerce true}}
                       {:normalise-single [wrap-single-type-check]})
  => {:account {:age 10}})
