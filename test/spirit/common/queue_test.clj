(ns spirit.common.queue-test
  (:use hara.test)
  (:require [spirit.common.queue :refer :all :as queue]
            [spirit.rabbitmq :as rabbitmq]))

(def routes {:queues    #{"q1" "q2"},
             :exchanges #{"ex1" "ex2"},
             :bindings   {"ex1" {:exchanges #{"ex2"},
                                 :queues #{"q1"}}
                          "ex2" {:exchanges #{}
                                 :queues #{"q2"}}}})

^{:refer spirit.common.queue/lengthen-topology :added "0.5"}
(fact "display routes in full"

  (lengthen-topology   {:queues    #{"q1" "q2"},
                        :exchanges #{"ex1" "ex2"},
                        :bindings   {"ex1" {:exchanges #{"ex2"},
                                            :queues #{"q1"}}
                                     "ex2" {:exchanges #{}
                                            :queues #{"q2"}}}})
  => (contains-in
      {:queues {"q1" map?
                "q2" map?},
       :exchanges {"ex1" map?
                   "ex2" map?},
       :bindings {"ex1" {:exchanges {"ex2" [map?]},
                         :queues {"q1" [map?]}},
                  "ex2" {:exchanges {},
                         :queues {"q2" [map?]}}}}))

^{:refer spirit.common.queue/shorten-topology :added "0.5"}
(fact "creates a shorthand version of the routing topology"

  (shorten-topology (lengthen-topology routes))
  => routes)

^{:refer spirit.common.queue/list-queues :added "0.5"}
(fact "returns current list of queues"

  (list-queues (queue/create {:routing routes}))
  => (contains {"q1" map?
                "q2" map?}))

^{:refer spirit.common.queue/add-queue :added "0.5"}
(fact "adds a queue to the mq"

  (-> (queue/create {:routing routes})
      (add-queue "q3")
      (list-queues))
  => (contains {"q1" map?
                "q2" map?
                "q3" map?}))

^{:refer spirit.common.queue/delete-queue :added "0.5"}
(fact "deletes a queue from the mq"

  (-> (queue/create {:routing routes})
      (delete-queue "q1")
      (list-queues))
  => (contains {"q2" map?}))

^{:refer spirit.common.queue/list-exchanges :added "0.5"}
(fact "returns current list of exchanges"

  (list-exchanges (queue/create {:routing routes}))
  => (contains {"ex1" map?
                "ex2" map?}))

^{:refer spirit.common.queue/add-exchange :added "0.5"}
(fact "adds an exchange to the mq"

  (-> (queue/create {:routing routes})
      (add-exchange "ex3")
      (list-exchanges))
  => (contains {"ex1" map?
                "ex2" map?
                "ex3" map?}))

^{:refer spirit.common.queue/delete-exchange :added "0.5"}
(fact "removes an exchange from the mq"

  (-> (queue/create {:routing routes})
      (delete-exchange "ex1")
      (list-exchanges))
  => (contains {"ex2" map?}))

^{:refer spirit.common.queue/list-bindings :added "0.5"}
(fact "returns current list of exchanges"

  (list-bindings (queue/create {:routing routes}))
  => (contains-in {"ex1" {:exchanges {"ex2" [map?]}
                          :queues {"q1" [map?]}}
                   "ex2" {:queues {"q2" [map?]}}}))

^{:refer spirit.common.queue/bind-exchange :added "0.5"}
(fact "binds a queue to the exchange"

  (-> (queue/create {:routing routes})
      (add-exchange "ex3")
      (bind-exchange "ex1" "ex3")
      (list-bindings))
  => (contains-in {"ex1" {:exchanges {"ex2" [map?]
                                      "ex3" [map?]}
                          :queues {"q1" [map?]}}
                   "ex2" {:queues {"q2" [map?]}}}))

^{:refer spirit.common.queue/bind-queue :added "0.5"}
(fact "binds an exchange to the exchange"

  (-> (queue/create {:routing routes})
      (add-queue "q3")
      (bind-queue "ex1" "q3")
      (list-bindings))
  => (contains-in {"ex1" {:exchanges {"ex2" [map?]}
                          :queues {"q1" [map?]
                                   "q3" [map?]}}
                   "ex2" {:queues {"q2" [map?]}}}))


^{:refer spirit.common.queue/routing :added "0.5"}
(fact "returns the routes for the current mq"
  
  (-> (queue/create {:routing routes})
      (routing)
      (shorten-topology))
  => routes)

(def consumers
  {"q1" {:hello {:sync true :function #(prn % :hello)}
         :world {:sync true :function #(prn % :world)}}
   "q2" {:foo {:sync true :function #(prn % :foo)}}})

^{:refer spirit.common.queue/list-consumers :added "0.5"}
(fact "lists all the consumers for the mq"

  (-> (queue/create {:routing routes :consumers consumers})
      (list-consumers))
  => (contains-in {"q1" {:hello map?,
                         :world map?},
                   "q2" {:foo map?}}))

^{:refer spirit.common.queue/add-consumer :added "0.5"}
(fact "adds a consumers to the mq"

  (-> (queue/create {:routing routes :consumers consumers})
      (add-consumer "q2" {:id :bar :sync true :function prn})
      (list-consumers))
  => (contains-in {"q1" {:hello map?,
                         :world map?},
                   "q2" {:foo map?
                         :bar map?}}))

^{:refer spirit.common.queue/delete-consumer :added "0.5"}
(fact "deletes the consumer from the queue"
  
  (-> (queue/create {:routing routes :consumers consumers})
      (delete-consumer "q1" :hello)
      (list-consumers))
  => (contains-in {"q1" {:world map?},
                   "q2" {:foo map?}}))

^{:refer spirit.common.queue/publish :added "0.5"}
(fact "publishes a message to an exchange"

  (def p (promise))
  
  (-> (queue/create {:routing routes
                     :consumers {"q1" {:hello {:function #(deliver p %)}}}})
      (publish "ex1" "hello there"))
  
  @p => "hello there")

^{:refer spirit.common.queue/install-bindings :added "0.5"}
(fact "installs bindings on the mq"
  (-> (queue/create {:routing {:queues #{"q1"}
                               :exchanges #{"ex1"}}})
      (install-bindings {"ex1" {:queues {"q1" [{}]}}})
      (list-bindings))
  => (contains-in {"ex1" {:queues {"q1" [map?]}}}))

^{:refer spirit.common.queue/install-routing :added "0.5"}
(fact "installs routing on the mq"
  (-> (queue/create)
      (install-routing routes)
      (routing {:short true}))
  => routes)

^{:refer spirit.common.queue/remove-routing :added "0.5"}
(fact "removes routing on the mq"
  
  (-> (queue/create {:routing routes})
      (add-queue "q3")
      (remove-routing)
      (routing {:short true}))
  => {:queues #{"q3"}, :exchanges #{}, :bindings {}})

^{:refer spirit.common.queue/purge-routing :added "0.5"}
(fact "clears all routing on the mq"
  
  (-> (queue/create {:routing routes})
      (purge-routing)
      (routing {:short true}))
  => {:queues #{}, :exchanges #{}, :bindings {}})

^{:refer spirit.common.queue/install-consumers :added "0.5"}
(fact "installs-consumers on the queues")

^{:refer spirit.common.queue/match-pattern :added "0.5"}
(fact "creates a re-pattern for the rabbitmq regex string"

  (match-pattern "*" "hello")
  => true

  (match-pattern ".*." ".hello.")
  => true)

^{:refer spirit.common.queue/route? :added "0.5"}
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


(comment
  (lucid.unit/import)
  )
