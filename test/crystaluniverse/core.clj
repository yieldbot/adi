(ns crystaluniverse.core
  (:require [cheshire.core :as json]
            [clojure.string :as st]))

(def customers-json (slurp "php/customers.json"))
(def address-json (slurp "php/address.json"))

(def customers (json/parse-string customers-json))
(def address (json/parse-string address-json))


(defn foo
  "I don't do a whole lot."
  [x]
  (println x "Hello, World!"))
