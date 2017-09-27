(ns adi.process.normalise.common.vector-test
  (:use midje.sweet)
  (:require [adi.process.normalise.base :as normalise]
            [adi.process.normalise.common.vector :refer :all]
            [adi.schema :as schema]
            [adi.test.examples :as examples]
            [adi.test.checkers :refer [raises-issue]]))

^{:refer adi.process.normalise.common.vector/wrap-attr-vector :added "0.3"}
(fact "wraps normalise with support for more complex expressions through use of double vector"

  (normalise/normalise {:account {:email [[":hello"]]}}
                       {:schema (schema/schema {:account/email [{:type :ref
                                                                :ref {:ns :email}}]})}
                       {:normalise-attr [wrap-attr-vector]
                        :normalise-single [wrap-single-vector]})
  => {:account {:email #db/id[:db.part/user -245025397]}})
