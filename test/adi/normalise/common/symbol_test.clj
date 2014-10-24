(ns adi.normalise.common.symbol-test
  (:use midje.sweet)
  (:require [adi.normalise.base :as normalise]
            [adi.normalise.common.symbol :refer :all]
            [adi.schema :as schema]
            [adi.test.examples :as examples]
            [adi.test.checkers :refer [raises-issue]]))

^{:refer adi.normalise.common.symbol/wrap-single-symbol :added "0.3"}
(fact "wraps normalise to work with symbols for queries as well as :ref attributes of datoms"

  (normalise/normalise {:account {:type 'hello}}
                       {:schema (schema/schema {:account/type [{:type :keyword
                                                                :keyword {:ns :account.type}}]})}
                       {:normalise-single [wrap-single-symbol]})
  => {:account {:type '?hello}})
