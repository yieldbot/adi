(ns spirit.data.exchange-test
  (:use hara.test)
  (:require [spirit.data.exchange :refer :all :as queue]
            [spirit.core.rabbitmq :as rabbitmq]))

(def routes {:queues    #{"q1" "q2"},
             :exchanges #{"ex1" "ex2"},
             :bindings   {"ex1" {:exchanges #{"ex2"},
                                 :queues #{"q1"}}
                          "ex2" {:exchanges #{}
                                 :queues #{"q2"}}}})

^{:refer spirit.data.exchange/lengthen-topology :added "0.5"}
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

^{:refer spirit.data.exchange/shorten-topology :added "0.5"}
(fact "creates a shorthand version of the routing topology"

  (shorten-topology (lengthen-topology routes))
  => routes)

^{:refer spirit.data.exchange/list-queues :added "0.5"}
(fact "returns current list of queues"

  (list-queues (queue/queue {:routing routes}))
  => (contains {"q1" map?
                "q2" map?}))

^{:refer spirit.data.exchange/add-queue :added "0.5"}
(fact "adds a queue to the mq"

  (-> (queue/queue {:routing routes})
      (add-queue "q3")
      (list-queues))
  => (contains {"q1" map?
                "q2" map?
                "q3" map?}))

^{:refer spirit.data.exchange/delete-queue :added "0.5"}
(fact "deletes a queue from the mq"

  (-> (queue/queue {:routing routes})
      (delete-queue "q1")
      (list-queues))
  => (contains {"q2" map?}))

^{:refer spirit.data.exchange/list-exchanges :added "0.5"}
(fact "returns current list of exchanges"

  (list-exchanges (queue/queue {:routing routes}))
  => (contains {"ex1" map?
                "ex2" map?}))

^{:refer spirit.data.exchange/add-exchange :added "0.5"}
(fact "adds an exchange to the mq"

  (-> (queue/queue {:routing routes})
      (add-exchange "ex3")
      (list-exchanges))
  => (contains {"ex1" map?
                "ex2" map?
                "ex3" map?}))

^{:refer spirit.data.exchange/delete-exchange :added "0.5"}
(fact "removes an exchange from the mq"

  (-> (queue/queue {:routing routes})
      (delete-exchange "ex1")
      (list-exchanges))
  => (contains {"ex2" map?}))

^{:refer spirit.data.exchange/list-bindings :added "0.5"}
(fact "returns current list of exchanges"

  (list-bindings (queue/queue {:routing routes}))
  => (contains-in {"ex1" {:exchanges {"ex2" [map?]}
                          :queues {"q1" [map?]}}
                   "ex2" {:queues {"q2" [map?]}}}))

^{:refer spirit.data.exchange/bind-exchange :added "0.5"}
(fact "binds a queue to the exchange"

  (-> (queue/queue {:routing routes})
      (add-exchange "ex3")
      (bind-exchange "ex1" "ex3")
      (list-bindings))
  => (contains-in {"ex1" {:exchanges {"ex2" [map?]
                                      "ex3" [map?]}
                          :queues {"q1" [map?]}}
                   "ex2" {:queues {"q2" [map?]}}}))

^{:refer spirit.data.exchange/bind-queue :added "0.5"}
(fact "binds an exchange to the exchange"

  (-> (queue/queue {:routing routes})
      (add-queue "q3")
      (bind-queue "ex1" "q3")
      (list-bindings))
  => (contains-in {"ex1" {:exchanges {"ex2" [map?]}
                          :queues {"q1" [map?]
                                   "q3" [map?]}}
                   "ex2" {:queues {"q2" [map?]}}}))


^{:refer spirit.data.exchange/routing :added "0.5"}
(fact "returns the routes for the current mq"
  
  (-> (queue/queue {:routing routes})
      (routing)
      (shorten-topology))
  => routes)

(def consumers
  {"q1" {:hello {:sync true :function #(prn % :hello)}
         :world {:sync true :function #(prn % :world)}}
   "q2" {:foo {:sync true :function #(prn % :foo)}}})

^{:refer spirit.data.exchange/list-consumers :added "0.5"}
(fact "lists all the consumers for the mq"

  (-> (queue/queue {:routing routes :consumers consumers})
      (list-consumers))
  => (contains-in {"q1" {:hello map?,
                         :world map?},
                   "q2" {:foo map?}}))

^{:refer spirit.data.exchange/add-consumer :added "0.5"}
(fact "adds a consumers to the mq"

  (-> (queue/queue {:routing routes :consumers consumers})
      (add-consumer "q2" {:id :bar :sync true :function prn})
      (list-consumers))
  => (contains-in {"q1" {:hello map?,
                         :world map?},
                   "q2" {:foo map?
                         :bar map?}}))

^{:refer spirit.data.exchange/delete-consumer :added "0.5"}
(fact "deletes the consumer from the queue"
  
  (-> (queue/queue {:routing routes :consumers consumers})
      (delete-consumer "q1" :hello)
      (list-consumers))
  => (contains-in {"q1" {:world map?},
                   "q2" {:foo map?}}))

^{:refer spirit.data.exchange/publish :added "0.5"}
(fact "publishes a message to an exchange"

  (def p (promise))
  
  (-> (queue/queue {:routing routes
                     :consumers {"q1" {:hello {:function #(deliver p %)}}}})
      (publish "ex1" "hello there"))
  
  @p => "hello there")

^{:refer spirit.data.exchange/install-bindings :added "0.5"}
(fact "installs bindings on the mq"
  (-> (queue/queue {:routing {:queues #{"q1"}
                               :exchanges #{"ex1"}}})
      (install-bindings {"ex1" {:queues {"q1" [{}]}}})
      (list-bindings))
  => (contains-in {"ex1" {:queues {"q1" [map?]}}}))

^{:refer spirit.data.exchange/install-routing :added "0.5"}
(fact "installs routing on the mq"
  (-> (queue/queue)
      (install-routing routes)
      (routing {:short true}))
  => routes)

^{:refer spirit.data.exchange/remove-routing :added "0.5"}
(fact "removes routing on the mq"
  
  (-> (queue/queue {:routing routes})
      (add-queue "q3")
      (remove-routing)
      (routing {:short true}))
  => {:queues #{"q3"}, :exchanges #{}, :bindings {}})

^{:refer spirit.data.exchange/purge-routing :added "0.5"}
(fact "clears all routing on the mq"
  
  (-> (queue/queue {:routing routes})
      (purge-routing)
      (routing {:short true}))
  => {:queues #{}, :exchanges #{}, :bindings {}})

^{:refer spirit.data.exchange/install-consumers :added "0.5"}
(fact "installs-consumers on the queues")

^{:refer spirit.data.exchange/match-pattern :added "0.5"}
(fact "creates a re-pattern for the rabbitmq regex string"

  (match-pattern "*" "hello")
  => true

  (match-pattern ".*." ".hello.")
  => true)

^{:refer spirit.data.exchange/route? :added "0.5"}
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
