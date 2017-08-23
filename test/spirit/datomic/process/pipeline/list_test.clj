(ns spirit.datomic.process.pipeline.list-test
  (:use hara.test)
  (:require [spirit.data.pipeline :as pipeline]
            [spirit.datomic.process.pipeline.list :refer :all]
            [spirit.data.schema :as schema]
            [data.examples :as examples]
            ))

^{:refer spirit.datomic.process.pipeline.list/wrap-single-list :added "0.3"}
(fact "wraps normalise with support for more complex expressions through use of double vector"

  (pipeline/normalise {:account {:age '(< ? 1)}}
                       {:schema (schema/schema {:account/age [{:type :long}]})}
                       {:normalise-single [wrap-single-list]})
  => {:account {:age '(< ? 1)}})