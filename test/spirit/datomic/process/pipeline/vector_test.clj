(ns spirit.core.datomic.process.pipeline.vector-test
  (:use hara.test)
  (:require [spirit.pipeline :as pipeline]
            [spirit.core.datomic.process.pipeline.id :as id]
            [spirit.core.datomic.process.pipeline.vector :refer :all]
            [spirit.schema :as schema]
            [data.examples :as examples]))

^{:refer spirit.core.datomic.process.pipeline.vector/wrap-attr-vector :added "0.3"}
(fact "wraps normalise with support for more complex expressions through use of double vector"

  (pipeline/normalise {:account {:email [[":hello"]]}}
                       {:schema (schema/schema {:account/email [{:type :ref
                                                                :ref {:ns :email}}]})}
                       {:normalise-attr [wrap-attr-vector]
                        :normalise-single [id/wrap-single-id wrap-single-vector]})
  => {:account {:email #db/id[:db.part/user -245025397]}})
