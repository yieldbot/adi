(ns spirit.core.rabbitmq-test
  (:use hara.test)
  (:require [spirit.data.exchange :as exchange]
            [spirit.core.rabbitmq :refer :all]))

(def routes {:queues    #{"q1" "q2"},
             :exchanges #{"ex1" "ex2"},
             :bindings   {"ex1" {:exchanges #{"ex2"},
                                 :queues #{"q1"}}
                          "ex2" {:exchanges #{}
                                 :queues #{"q2"}}}})

^{:refer spirit.core.rabbitmq/list-queues :added "0.5"}
(fact "returns current list of queues"

  (exchange/list-queues (exchange/exchange {:type :rabbitmq
                                            :routing routes}))
  => (contains {"q1" map?
                "q2" map?}))

^{:refer spirit.core.rabbitmq/add-queue :added "0.5"}
(fact "adds a queue to the mq"

  (-> (exchange/exchange {:type :rabbitmq
                          :routing routes
                          :refresh true})
      (exchange/add-queue "q3")
      (exchange/list-queues))
  => (contains {"q1" map?
                "q2" map?
                "q3" map?}))

^{:refer spirit.core.rabbitmq/delete-queue :added "0.5"}
(fact "deletes a queue from the mq"

  (-> (exchange/exchange {:type :rabbitmq
                          :routing routes
                          :refresh true})
      (exchange/delete-queue "q1")
      (exchange/list-queues))
  => (contains {"q2" map?}))

^{:refer spirit.core.rabbitmq/list-exchanges :added "0.5"}
(fact "returns current list of exchanges"

  (-> (exchange/exchange {:type :rabbitmq
                          :routing routes
                          :refresh true})
      (exchange/list-exchanges))
  => (contains {"ex1" map?
                "ex2" map?}))

^{:refer spirit.core.rabbitmq/add-exchange :added "0.5"}
(fact "adds an exchange to the mq"

  (-> (exchange/exchange {:type :rabbitmq
                          :routing routes
                          :refresh true})
      (exchange/add-exchange "ex3")
      (exchange/list-exchanges))
  => (contains {"ex1" map?
                "ex2" map?
                "ex3" map?}))

^{:refer spirit.core.rabbitmq/delete-exchange :added "0.5"}
(fact "removes an exchange from the mq"

  (-> (exchange/exchange {:type :rabbitmq
                          :routing routes
                          :refresh true})
      (exchange/delete-exchange "ex1")
      (exchange/list-exchanges))
  => (contains {"ex2" map?}))

^{:refer spirit.core.rabbitmq/list-bindings :added "0.5"}
(fact "returns current list of exchanges"

  (-> (exchange/exchange {:type :rabbitmq
                          :routing routes
                          :refresh true})
      (exchange/list-bindings))
  => (contains-in {"ex1" {:exchanges {"ex2" [map?]}
                          :queues {"q1" [map?]}}
                   "ex2" {:queues {"q2" [map?]}}}))

^{:refer spirit.core.rabbitmq/bind-exchange :added "0.5"}
(fact "returns current list of exchanges"

  (-> (exchange/exchange {:type :rabbitmq
                          :routing routes
                          :refresh true})
      (exchange/add-exchange "ex3")
      (exchange/bind-exchange "ex1" "ex3")
      (exchange/list-bindings))
  => (contains-in {"ex1" {:exchanges {"ex2" [map?]
                                      "ex3" [map?]}
                          :queues {"q1" [map?]}}
                   "ex2" {:queues {"q2" [map?]}}}))

^{:refer spirit.core.rabbitmq/bind-queue :added "0.5"}
(fact "returns current list of exchanges"

  (-> (exchange/exchange {:type :rabbitmq
                          :routing routes
                          :refresh true})
      (exchange/add-queue "q3")
      (exchange/bind-queue "ex1" "q3")
      (exchange/list-bindings))
  => (contains-in {"ex1" {:exchanges {"ex2" [map?]}
                          :queues {"q1" [map?]
                                   "q3" [map?]}}
                   "ex2" {:queues {"q2" [map?]}}}))

^{:refer spirit.core.rabbitmq/routing-all :added "0.5"}
(fact "lists all the routing in the mq"

  (routing-all (exchange/exchange {:type :rabbitmq
                                   :refresh true})
               {})
  => {"/" {:queues {}, :exchanges {}, :bindings {}}})

^{:refer spirit.core.rabbitmq/network :added "0.5"}
(fact "returns the mq network"

  (network (exchange/exchange {:type :rabbitmq
                               :refresh true}))
  => (contains-in {:cluster-name string?
                   :nodes [string?]
                   :vhosts ["/"]
                   :connections ()
                   :channels {}}))

^{:refer spirit.core.rabbitmq/install-vhost :added "0.5"}
(fact "installs vhost and adds user permissions")

^{:refer spirit.core.rabbitmq/install-consumers :added "0.5"}
(fact "install consumers")

^{:refer spirit.core.rabbitmq/rabbit :added "0.5"}
(fact "creates a rabbitmq instance")
