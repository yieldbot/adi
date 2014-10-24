(ns adi.normalise.common.set-test
  (:use midje.sweet)
  (:require [adi.normalise.base :as normalise]
            [adi.normalise.common.set :refer :all]
            [adi.schema :as schema]
            [adi.test.examples :as examples]
            [adi.test.checkers :refer [raises-issue]]))

^{:refer adi.normalise.common.set/wrap-attr-set :added "0.3"}
(fact "wraps normalise to type check inputs as well as to coerce incorrect inputs"
  (normalise/normalise {:account {:tags "10"}}
                       {:schema (schema/schema examples/account-orders-items-image)}
                       {:normalise-attr [wrap-attr-set]})
  => {:account {:tags #{"10"}}}

  (normalise/normalise {:account {:user #{"andy" "bob"}}}
                       {:schema (schema/schema examples/account-orders-items-image)
                        :type "query"}
                       {:normalise-attr [wrap-attr-set]})
  => {:account {:user #{"bob" "andy"}}})
