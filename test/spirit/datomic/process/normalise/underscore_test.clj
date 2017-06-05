(ns spirit.datomic.process.normalise.underscore-test
  (:use hara.test)
  (:require [spirit.common.normalise :as normalise]
            [spirit.datomic.process.normalise.underscore :refer :all]
            [spirit.common.schema :as schema]
            [data.examples :as examples]))

^{:refer spirit.datomic.process.normalise.underscore/rep-key :added "0.3"}
(fact "finds the :required or :representative key within a schema,
  otherwise throws an error"
  (rep-key (:account examples/account-orders-items-image))
  => [:user]

  (rep-key (:order examples/account-orders-items-image))
  => (throws-info {:needs-require-key true}))

^{:refer spirit.datomic.process.normalise.underscore/wrap-branch-underscore :added "0.3"}
(fact "wraps normalise to process underscores"
  (normalise/normalise {:account '_}
                       {:schema (schema/schema examples/account-orders-items-image)
                        :command :query}
                       {:normalise-branch [wrap-branch-underscore]})
  => {:account {:user '#{_}}})
