(ns spirit.datomic.process.pack.review-test
  (:use hara.test)
  (:require [spirit.datomic.process.pack.review :refer :all]
            [spirit.datomic.process.pack.analyse :as analyse]
            [spirit.common.schema :as schema]))

^{:refer spirit.datomic.process.pack.review/review-raw :added "0.3"}
(fact "checks for required data"
  
  (review-raw {:# {:nss #{:account} :account/name "Chris"}}
              {:schema (schema/schema {:account {:name [{:required true}]
                                                 :age  [{:required true}]}})
               :options {:schema-required true}})
  => (throws))
