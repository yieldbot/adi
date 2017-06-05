(ns spirit.common.normalise.base.keyword-test
  (:use hara.test)
  (:require [spirit.common.normalise :as normalise]
            [spirit.common.normalise.base.keyword :refer :all]
            [spirit.common.schema :as schema]
            [data.examples :as examples]))

^{:refer spirit.common.normalise.base.keyword/wrap-single-keyword :added "0.3"}
(fact "removes the keyword namespace if there is one"

  (normalise/normalise {:account {:type :account.type/vip}}
                       {:schema (schema/schema {:account/type [{:type :keyword
                                                                :keyword {:ns :account.type}}]})}
                       {:normalise-single [wrap-single-keyword]})
  => {:account {:type :vip}})
