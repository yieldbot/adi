(ns adi.test-json
  (:use midje.sweet
        adi.utils
        [adi.data :only [iid]])
  (:require [adi.core :as adi]
            [cheshire.core :as json]
            [clj-time.core :as t]
            [clj-time.coerce :as tc]
            [datomic.api :as d]))

(def account-map
        {:ac {:name  [{:type        :string}]
              :pass  [{:type        :string}]
              :tags  [{:type        :string
                      :cardinality :many}]}})

(defn kw-keys
  ([m] (kw-keys m {}))
  ([m output]
     (if-let [[k v] (first m)]
       (cond (hash-map? v)
             (kw-keys (next m)
                      (->> (kw-keys v)
                           (assoc output (keyword k))))
             :else
             (kw-keys (next m) (assoc output (keyword k) v)))
       output)))



(def a
  (kw-keys (json/parse-string (json/generate-string {:a (java.util.Date.)}))))

(tc/to-date (a :a))
(tc/to-date-time (a :a))
(type (a "a"))
