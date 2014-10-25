(ns adi.process.normalise.common.enum-test
  (:use midje.sweet)
  (:require [adi.process.normalise.base :as normalise]
            [adi.process.normalise.common.enum :refer :all]
            [adi.schema :as schema]
            [adi.test.examples :as examples]
            [adi.test.checkers :refer [raises-issue]]))

^{:refer adi.process.normalise.common.enum/wrap-single-enum :added "0.3"}
(fact "wraps normalise with comprehension of the enum type"

  (normalise/normalise {:account {:type :account.type/guest}}
                       {:schema (schema/schema {:account/type [{:type :enum
                                                                :enum {:ns :account.type
                                                                       :values #{:vip :guest}}}]})}
                       {:normalise-single [wrap-single-enum]})
  => {:account {:type :guest}}
  ^:hidden
  (normalise/normalise {:account {:type :account.type/WRONG}}
                       {:schema (schema/schema {:account/type [{:type :enum
                                                                :enum {:ns :account.type
                                                                       :values #{:vip :guest}}}]})}
                       {:normalise-single [wrap-single-enum]})
  => (raises-issue {:check #{:vip :guest}
                    :data :account.type/WRONG
                    :wrong-input true}))
