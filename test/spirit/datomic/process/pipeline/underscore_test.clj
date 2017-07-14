(ns spirit.core.datomic.process.pipeline.underscore-test
  (:use hara.test)
  (:require [spirit.pipeline :as pipeline]
            [spirit.core.datomic.process.pipeline.underscore :refer :all]
            [spirit.schema :as schema]
            [data.examples :as examples]))

^{:refer spirit.core.datomic.process.pipeline.underscore/rep-key :added "0.3"}
(fact "finds the :required or :representative key within a schema,
  otherwise throws an error"
  (rep-key (:account examples/account-orders-items-image))
  => [:user]

  (rep-key (:order examples/account-orders-items-image))
  => (throws-info {:needs-require-key true}))

^{:refer spirit.core.datomic.process.pipeline.underscore/wrap-branch-underscore :added "0.3"}
(fact "wraps normalise to process underscores"
  (pipeline/normalise {:account '_}
                       {:schema (schema/schema examples/account-orders-items-image)
                        :command :query}
                       {:normalise-branch [wrap-branch-underscore]})
  => {:account {:user '#{_}}})
