(ns adi.normalise.common.underscore-test
  (:use midje.sweet)
  (:require [adi.normalise.base :as normalise]
            [adi.normalise.common.underscore :refer :all]
            [adi.schema :as schema]
            [adi.test.examples :as examples]
            [adi.test.checkers :refer [raises-issue]]))

^{:refer adi.normalise.common.underscore/rep-key :added "0.3"}
(fact "finds the :required or :representative key within a schema,
  otherwise throws an error"
  (rep-key (:account examples/account-orders-items-image))
  => [:user]

  (rep-key (:order examples/account-orders-items-image))
  => (raises-issue {:needs-require-key true}))

^{:refer adi.normalise.common.underscore/wrap-branch-underscore :added "0.3"}
(fact "wraps normalise to process underscores"
  (normalise/normalise {:account '_}
                       {:schema (schema/schema examples/account-orders-items-image)
                        :type "query"}
                       {:normalise-branch [wrap-branch-underscore]})
  => {:account {:user '#{_}}})
