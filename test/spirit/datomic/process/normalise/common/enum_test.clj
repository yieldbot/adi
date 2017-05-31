(ns spirit.datomic.process.normalise.common.enum-test
  (:use hara.test)
  (:require [spirit.datomic.process.normalise.base :as normalise]
            [spirit.datomic.process.normalise.common.enum :refer :all]
            [spirit.common.schema :as schema]
            [data.examples :as examples]
            ))

^{:refer spirit.datomic.process.normalise.common.enum/wrap-single-enum :added "0.3"}
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
  => (throws-info {:check #{:vip :guest}
                    :data :account.type/WRONG
                    :wrong-input true}))
