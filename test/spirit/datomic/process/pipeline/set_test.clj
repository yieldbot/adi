(ns spirit.core.datomic.process.pipeline.set-test
  (:use hara.test)
  (:require [spirit.pipeline :as pipeline]
            [spirit.core.datomic.process.pipeline.set :refer :all]
            [spirit.schema :as schema]
            [data.examples :as examples]))

^{:refer spirit.core.datomic.process.pipeline.set/wrap-attr-set :added "0.3"}
(fact "wraps normalise to type check inputs as well as to coerce incorrect inputs"
  (pipeline/normalise {:account {:tags "10"}}
                       {:schema (schema/schema examples/account-orders-items-image)}
                       {:normalise-attr [wrap-attr-set]})
  => {:account {:tags #{"10"}}}

  (pipeline/normalise {:account {:user #{"andy" "bob"}}}
                       {:schema (schema/schema examples/account-orders-items-image)}
                       {:normalise-attr [wrap-attr-set]})
  => (throws-info {:normalise true,
                   :wrong-input true,
                   :data #{"bob" "andy"},
                   :nsv [:account :user],
                   :key-path nil})
  
  (pipeline/normalise {:account {:user #{"andy" "bob"}}}
                       {:schema (schema/schema examples/account-orders-items-image)
                        :command :query}
                       {:normalise-attr [wrap-attr-set]})
  => {:account {:user #{"bob" "andy"}}})
