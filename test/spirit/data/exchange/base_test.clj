(ns spirit.data.exchange.base-test
  (:use hara.test)
  (:require [spirit.data.exchange.base :refer :all]
            [spirit.data.exchange.common :as common]
            [spirit.protocol.iexchange :as exchange]
            [hara.component :as component]))

^{:refer spirit.data.exchange.base/match-pattern :added "0.5"}
(fact "creates a re-pattern for the rabbitmq regex string"

  (match-pattern "*" "hello")
  => true

  (match-pattern ".*." ".hello.")
  => true)

^{:refer spirit.data.exchange.base/route? :added "0.5"}
(fact "checks if a message will be routed"

  (route? {:type "fanout"} {} {})
  => true

  (route? {:type "topic"}
          {:key "user.account.login"}
          {:routing-key "*.account.*"})
  => true

  (route? {:type "header"}
          {:headers {"account" "login"}}
          {:arguments {"account" "*"}})
  => true)

^{:refer spirit.data.exchange.base/add-in-atom :added "0.5"}
(comment "helper for adding queues and exchanges")

^{:refer spirit.data.exchange.base/add-queue-atom :added "0.5"}
(fact "adds a queue to the atom"

  (-> (atom {})
      (add-queue-atom "q3" {})
      (add-queue-atom "q2" {}) 
      ((comp sort keys :queues deref)))
  => ["q2" "q3"])

^{:refer spirit.data.exchange.base/add-exchange-atom :added "0.5"}
(fact "adds a queue to the atom"

  (-> (atom {})
      (add-exchange-atom "ex1" {})
      (add-exchange-atom "ex2" {}) 
      ((comp sort keys :exchanges deref)))
  => ["ex1" "ex2"])

^{:refer spirit.data.exchange.base/list-in-atom :added "0.5"}
(comment "helper for listing queues and exchanges"
  (-> (atom {})
      (add-queue-atom "q3" {}) 
      (list-in-atom :queues))
  => {"q3" {:exclusive false,
            :auto-delete false,
            :durable false}})

^{:refer spirit.data.exchange.base/list-in-atom :added "0.5"}
(fact "returns current list of queues"
  (-> (atom {})
      (add-queue-atom "q3" {}) 
      (list-in-atom :queues))
  => {"q3" {:exclusive false
            :auto-delete false
            :durable false}})

^{:refer spirit.data.exchange.base/delete-in-atom :added "0.5"}
(fact "returns current list of queues"
  (-> (atom {})
      (add-exchange-atom "ex1" {})
      (add-exchange-atom "ex2" {})
      (delete-in-atom :exchanges "ex1")
      (list-in-atom :exchanges))
  => {"ex2" {:type "topic"
             :internal false
             :auto-delete false
             :durable true}})

(def routes {:queues    #{"q1" "q2"},
             :exchanges #{"ex1" "ex2"},
             :bindings   {"ex1" {:exchanges #{"ex2"},
                                 :queues #{"q1"}}
                          "ex2" {:exchanges #{}
                                 :queues #{"q2"}}}})

^{:refer spirit.data.exchange.base/list-bindings-atom :added "0.5"}
(fact "returns current list of exchanges"

  (-> (atom {})
      (common/install-routing routes)
      (list-bindings-atom))
  => (contains-in {"ex1" {:exchanges {"ex2" [map?]}
                          :queues {"q1" [map?]}}
                   "ex2" {:queues {"q2" [map?]}}}))

^{:refer spirit.data.exchange.base/bind-in-atom :added "0.5"}
(fact "helper function for binding to queues and exchanges")

^{:refer spirit.data.exchange.base/bind-exchange-atom :added "0.5"}
(fact "binds a queue to the exchange"

  (-> (atom {})
      (common/install-routing routes)
      (add-exchange-atom "ex3" {})
      (bind-exchange-atom "ex1" "ex3" {})
      (list-bindings-atom))
  => (contains-in {"ex1" {:exchanges {"ex2" [map?]
                                      "ex3" [map?]}
                          :queues {"q1" [map?]}}
                   "ex2" {:queues {"q2" [map?]}}}))

^{:refer spirit.data.exchange.base/bind-queue-atom :added "0.5"}
(fact "binds an exchange to a queue"

  (-> (atom {})
      (common/install-routing routes)
      (add-queue-atom "q3" {})
      (bind-queue-atom "ex1" "q3" {})
      (list-bindings-atom))
  => (contains-in {"ex1" {:exchanges {"ex2" [map?]}
                          :queues {"q1" [map?]
                                   "q3" [map?]}}
                   "ex2" {:queues {"q2" [map?]}}}))

^{:refer spirit.data.exchange.base/list-consumers-atom :added "0.5"}
(fact "lists all connected consumers"

  (-> (atom {})
      (common/install-routing routes)
      (add-consumer-atom  "q2" {:id :bar
                                :sync true
                                :function prn})
      (list-consumers-atom))
  => (contains-in {"q1" {}
                   "q2" {:bar {:id :bar,
                               :sync true,
                               :function fn?}}}))

^{:refer spirit.data.exchange.base/add-consumer-atom :added "0.5"}
(comment "adds a consumer to the queue")

^{:refer spirit.data.exchange.base/delete-consumer-atom :added "0.5"}
(comment "deletes a consumer to the queue")

^{:refer spirit.data.exchange.base/publish-atom :added "0.5"}
(fact "publishes a message to the exchange"

  (def p (promise))
  
  (-> (atom {})
      (common/install-routing routes)
      (add-consumer-atom "q1" {:id :bar
                               :sync true
                               :function #(deliver p %)})
      (publish-atom "ex1" "hello there" {}))
  
  @p => "hello there")
