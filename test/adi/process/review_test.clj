(ns adi.process.review-test
  (:use midje.sweet)
  (:require [adi.process.review :as review]
            [adi.process.analyse :as analyse]
            [adi.schema :as schema]
            [adi.test.examples :as examples]))

(fact
  (review {:# {:nss #{:account} :account/name "Chris"}}
          {:schema (schema/schema {:account {:name [{:required true}]
                                             :age  [{:required true}]}})
           :options {:schema-required true}})
  => (throws))
