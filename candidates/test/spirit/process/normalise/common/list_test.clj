(ns spirit.process.normalise.common.list-test
  (:use hara.test)
  (:require [spirit.process.normalise.base :as normalise]
            [spirit.process.normalise.common.list :refer :all]
            [spirit.schema :as schema]
            [spirit.test.examples :as examples]
            [spirit.test.checkers :refer [raises-issue]]))

^{:refer spirit.process.normalise.common.list/wrap-single-list :added "0.3"}
(fact "wraps normalise with support for more complex expressions through use of double vector"

  (normalise/normalise {:account {:age '(< ? 1)}}
                       {:schema (schema/schema {:account/age [{:type :long}]})}
                       {:normalise-single [wrap-single-list]})
  => {:account {:age '(< ? 1)}})