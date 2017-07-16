(ns spirit.data.pipeline.base.enum-test
  (:use hara.test)
  (:require [spirit.data.pipeline :as pipeline]
            [spirit.data.pipeline.base.enum :refer :all]
            [spirit.data.schema :as schema]
            [data.examples :as examples]))

^{:refer spirit.data.pipeline.base.enum/wrap-single-enum :added "0.3"}
(fact "wraps normalise with comprehension of the enum type"

  (pipeline/normalise {:account {:type :account.type/guest}}
                       {:schema (schema/schema {:account/type [{:type :enum
                                                                :enum {:ns :account.type
                                                                       :values #{:vip :guest}}}]})}
                       {:normalise-single [wrap-single-enum]})
  => {:account {:type :guest}}
  ^:hidden
  (pipeline/normalise {:account {:type :account.type/WRONG}}
                       {:schema (schema/schema {:account/type [{:type :enum
                                                                :enum {:ns :account.type
                                                                       :values #{:vip :guest}}}]})}
                       {:normalise-single [wrap-single-enum]})
  => (throws-info {:check #{:vip :guest}
                    :data :account.type/WRONG
                    :wrong-input true}))
