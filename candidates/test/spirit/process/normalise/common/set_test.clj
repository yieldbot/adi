(ns spirit.process.normalise.common.set-test
  (:use hara.test)
  (:require [spirit.process.normalise.base :as normalise]
            [spirit.process.normalise.common.set :refer :all]
            [spirit.schema :as schema]
            [spirit.test.examples :as examples]
            [spirit.test.checkers :refer [raises-issue]]))

^{:refer spirit.process.normalise.common.set/wrap-attr-set :added "0.3"}
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
