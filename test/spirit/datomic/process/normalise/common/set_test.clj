(ns spirit.datomic.process.normalise.common.set-test
  (:use hara.test)
  (:require [spirit.datomic.process.normalise.base :as normalise]
            [spirit.datomic.process.normalise.common.set :refer :all]
            [spirit.common.schema :as schema]
            [data.examples :as examples]))

^{:refer spirit.datomic.process.normalise.common.set/wrap-attr-set :added "0.3"}
(fact "wraps normalise to type check inputs as well as to coerce incorrect inputs"
  (normalise/normalise {:account {:tags "10"}}
                       {:schema (schema/schema examples/account-orders-items-image)}
                       {:normalise-attr [wrap-attr-set]})
  => {:account {:tags #{"10"}}}

  (normalise/normalise {:account {:user #{"andy" "bob"}}}
                       {:schema (schema/schema examples/account-orders-items-image)}
                       {:normalise-attr [wrap-attr-set]})
  => (throws-info {:normalise true,
                   :wrong-input true,
                   :data #{"bob" "andy"},
                   :nsv [:account :user],
                   :key-path nil})
  
  (normalise/normalise {:account {:user #{"andy" "bob"}}}
                       {:schema (schema/schema examples/account-orders-items-image)
                        :command :query}
                       {:normalise-attr [wrap-attr-set]})
  => {:account {:user #{"bob" "andy"}}})
