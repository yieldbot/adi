(ns spirit.process.pack.review-test
  (:use hara.test)
  (:require [spirit.process.pack.review :refer :all]
            [spirit.process.pack.analyse :as analyse]
            [spirit.schema :as schema]
            [spirit.test.examples :as examples]))


^{:refer spirit.process.pack.review/review :added "0.3"}
(fact ""
  (review {:# {:nss #{:account} :account/name "Chris"}}
          {:schema (schema/schema {:account {:name [{:required true}]
                                             :age  [{:required true}]}})
           :options {:schema-required true}})
  => (throws))
