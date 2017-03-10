(ns adi.process.pack.review-test
  (:use midje.sweet)
  (:require [adi.process.pack.review :refer :all]
            [adi.process.pack.analyse :as analyse]
            [adi.schema :as schema]
            [adi.test.examples :as examples]))


^{:refer adi.process.pack.review/review :added "0.3"}
(fact ""
  (review {:# {:nss #{:account} :account/name "Chris"}}
          {:schema (schema/schema {:account {:name [{:required true}]
                                             :age  [{:required true}]}})
           :options {:schema-required true}})
  => (throws))
