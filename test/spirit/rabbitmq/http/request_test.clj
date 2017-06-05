(ns spirit.rabbitmq.http.request-test
  (:use hara.test)
  (:require [spirit.rabbitmq.http.request :refer :all]
            [spirit.rabbitmq.http :as http]
            [cheshire.core :as json]))

^{:refer spirit.rabbitmq.http.request/create-url :added "0.5"}
(fact "creates the management url"

  (create-url http/*default-options* "hello")
  => "http://localhost:15672/api/hello")

^{:refer spirit.rabbitmq.http.request/wrap-parse-json :added "0.5"}
(fact "returns the body as a clojure map"

  ((wrap-parse-json identity)
   {:status 200
    :body (json/generate-string {:a 1 :b 2})})
  => {:a 1, :b 2})

^{:refer spirit.rabbitmq.http.request/update-nested-keys :added "0.5"}
(fact "updates keys in the nesting"
  
  (update-nested-keys {:a {:b {:c 1}}}
                      #(keyword (str (name %) "-boo")))
  => {:a-boo {:b-boo {:c-boo 1}}})

^{:refer spirit.rabbitmq.http.request/wrap-generate-json :added "0.5"}
(fact "returns the body as a json string"
  
  ((wrap-generate-json identity)
   {:status 200
    :body {:a 1 :b 2}})
  => {:status 200, :body "{\"a\":1,\"b\":2}"})

^{:refer spirit.rabbitmq.http.request/request :added "0.5"}
(fact "creates request for the rabbitmq management api"

  (request http/*default-options* "cluster-name")
  => (contains {:name string?})  )
