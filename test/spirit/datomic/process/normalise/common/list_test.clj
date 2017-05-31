(ns spirit.datomic.process.normalise.common.list-test
  (:use hara.test)
  (:require [spirit.datomic.process.normalise.base :as normalise]
            [spirit.datomic.process.normalise.common.list :refer :all]
            [spirit.common.schema :as schema]
            [data.examples :as examples]
            ))

^{:refer spirit.datomic.process.normalise.common.list/wrap-single-list :added "0.3"}
(fact "wraps normalise with support for more complex expressions through use of double vector"

  (normalise/normalise {:account {:age '(< ? 1)}}
                       {:schema (schema/schema {:account/age [{:type :long}]})}
                       {:normalise-single [wrap-single-list]})
  => {:account {:age '(< ? 1)}})