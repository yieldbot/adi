(ns spirit.rabbitmq-test
  (:use hara.test)
  (:require [spirit.rabbitmq :refer :all]
            [spirit.common.queue :as queue]))

(def routes {:queues    #{"q1" "q2"},
             :exchanges #{"ex1" "ex2"},
             :bindings   {"ex1" {:exchanges #{"ex2"},
                                 :queues #{"q1"}}
                          "ex2" {:exchanges #{}
                                 :queues #{"q2"}}}})

^{:refer spirit.rabbitmq/list-queues :added "0.5"}
(fact "returns current list of queues"

  (queue/list-queues (queue/queue {:type :rabbitmq
                                    :routing routes}))
  => (contains {"q1" map?
                "q2" map?}))

^{:refer spirit.rabbitmq/add-queue :added "0.5"}
(fact "adds a queue to the mq"

  (-> (queue/queue {:type :rabbitmq
                     :routing routes
                     :refresh true})
      (queue/add-queue "q3")
      (queue/list-queues))
  => (contains {"q1" map?
                "q2" map?
                "q3" map?}))

^{:refer spirit.rabbitmq/delete-queue :added "0.5"}
(fact "deletes a queue from the mq"

  (-> (queue/queue {:type :rabbitmq
                     :routing routes
                     :refresh true})
      (queue/delete-queue "q1")
      (queue/list-queues))
  => (contains {"q2" map?}))

^{:refer spirit.rabbitmq/list-exchanges :added "0.5"}
(fact "returns current list of exchanges"

  (-> (queue/queue {:type :rabbitmq
                     :routing routes
                     :refresh true})
      (queue/list-exchanges))
  => (contains {"ex1" map?
                "ex2" map?}))

^{:refer spirit.rabbitmq/add-exchange :added "0.5"}
(fact "adds an exchange to the mq"

  (-> (queue/queue {:type :rabbitmq
                     :routing routes
                     :refresh true})
      (queue/add-exchange "ex3")
      (queue/list-exchanges))
  => (contains {"ex1" map?
                "ex2" map?
                "ex3" map?}))

^{:refer spirit.rabbitmq/delete-exchange :added "0.5"}
(fact "removes an exchange from the mq"

  (-> (queue/queue {:type :rabbitmq
                     :routing routes
                     :refresh true})
      (queue/delete-exchange "ex1")
      (queue/list-exchanges))
  => (contains {"ex2" map?}))

^{:refer spirit.rabbitmq/list-bindings :added "0.5"}
(fact "returns current list of exchanges"

  (-> (queue/queue {:type :rabbitmq
                     :routing routes
                     :refresh true})
      (queue/list-bindings))
  => (contains-in {"ex1" {:exchanges {"ex2" [map?]}
                          :queues {"q1" [map?]}}
                   "ex2" {:queues {"q2" [map?]}}}))

^{:refer spirit.rabbitmq/bind-exchange :added "0.5"}
(fact "returns current list of exchanges"

  (-> (queue/queue {:type :rabbitmq
                     :routing routes
                     :refresh true})
      (queue/add-exchange "ex3")
      (queue/bind-exchange "ex1" "ex3")
      (queue/list-bindings))
  => (contains-in {"ex1" {:exchanges {"ex2" [map?]
                                      "ex3" [map?]}
                          :queues {"q1" [map?]}}
                   "ex2" {:queues {"q2" [map?]}}}))

^{:refer spirit.rabbitmq/bind-queue :added "0.5"}
(fact "returns current list of exchanges"

  (-> (queue/queue {:type :rabbitmq
                     :routing routes
                     :refresh true})
      (queue/add-queue "q3")
      (queue/bind-queue "ex1" "q3")
      (queue/list-bindings))
  => (contains-in {"ex1" {:exchanges {"ex2" [map?]}
                          :queues {"q1" [map?]
                                   "q3" [map?]}}
                   "ex2" {:queues {"q2" [map?]}}}))

^{:refer spirit.rabbitmq/routing-all :added "0.5"}
(fact "lists all the routing in the mq"

  (routing-all (queue/queue {:type :rabbitmq
                              :refresh true})
               {})
  => {"/" {:queues {}, :exchanges {}, :bindings {}}})

^{:refer spirit.rabbitmq/network :added "0.5"}
(fact "returns the mq network"

  (network (queue/queue {:type :rabbitmq
                          :refresh true}))
  => (contains-in {:cluster-name string?
                   :nodes [string?]
                   :vhosts ["/"]
                   :connections ()
                   :channels {}}))

^{:refer spirit.rabbitmq/install-vhost :added "0.5"}
(fact "installs vhost and adds user permissions")

^{:refer spirit.rabbitmq/install-consumers :added "0.5"}
(fact "install consumers")

^{:refer spirit.rabbitmq/rabbit :added "0.5"}
(fact "creates a rabbitmq instance")
