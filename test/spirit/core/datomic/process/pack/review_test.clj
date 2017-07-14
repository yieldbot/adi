(ns spirit.core.datomic.process.pack.review-test
  (:use hara.test)
  (:require [spirit.core.datomic.process.pack.review :refer :all]
            [spirit.core.datomic.process.pack.analyse :as analyse]
            [spirit.schema :as schema]))

^{:refer spirit.core.datomic.process.pack.review/review-raw :added "0.3"}
(fact "checks for required data"
  
  (review-raw {:# {:nss #{:account} :account/name "Chris"}}
              {:schema (schema/schema {:account {:name [{:required true}]
                                                 :age  [{:required true}]}})
               :options {:schema-required true}})
  => (throws))
