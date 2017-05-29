(ns spirit.process.normalise.common.underscore-test
  (:use hara.test)
  (:require [spirit.process.normalise.base :as normalise]
            [spirit.process.normalise.common.underscore :refer :all]
            [spirit.schema :as schema]
            [spirit.test.examples :as examples]
            [spirit.test.checkers :refer [raises-issue]]))

^{:refer spirit.process.normalise.common.underscore/rep-key :added "0.3"}
(fact "finds the :required or :representative key within a schema,
  otherwise throws an error"
  (rep-key (:account examples/account-orders-items-image))
  => [:user]

  (rep-key (:order examples/account-orders-items-image))
  => (raises-issue {:needs-require-key true}))

^{:refer spirit.process.normalise.common.underscore/wrap-branch-underscore :added "0.3"}
(fact "wraps normalise to process underscores"
  (normalise/normalise {:account '_}
                       {:schema (schema/schema examples/account-orders-items-image)
                        :type "query"}
                       {:normalise-branch [wrap-branch-underscore]})
  => {:account {:user '#{_}}})
