(ns spirit.data.exchange-test
  (:use hara.test)
  (:require [spirit.data.exchange :refer :all :as exchange]))

(def routes {:queues    #{"q1" "q2"},
             :exchanges #{"ex1" "ex2"},
             :bindings   {"ex1" {:exchanges #{"ex2"},
                                 :queues #{"q1"}}
                          "ex2" {:exchanges #{}
                                 :queues #{"q2"}}}})

^{:refer spirit.data.exchange/list-queues :added "0.5"}
(fact "returns current list of queues"

  (list-queues (exchange {:type :mock
                          :routing routes}))
  => (contains {"q1" map?
                "q2" map?}))

^{:refer spirit.data.exchange/add-queue :added "0.5"}
(fact "adds a queue to the mq"

  (-> (exchange {:type :mock
                 :routing routes})
      (add-queue "q3")
      (list-queues))
  => (contains {"q1" map?
                "q2" map?
                "q3" map?}))

^{:refer spirit.data.exchange/delete-queue :added "0.5"}
(fact "deletes a queue from the mq"

  (-> (exchange {:type :mock
                 :routing routes})
      (delete-queue "q1")
      (list-queues))
  => (contains {"q2" map?}))

^{:refer spirit.data.exchange/list-exchanges :added "0.5"}
(fact "returns current list of exchanges"

  (list-exchanges (exchange {:type :mock
                             :routing routes}))
  => (contains {"ex1" map?
                "ex2" map?}))

^{:refer spirit.data.exchange/add-exchange :added "0.5"}
(fact "adds an exchange to the mq"

  (-> (exchange {:type :mock
                 :routing routes})
      (add-exchange "ex3")
      (list-exchanges))
  => (contains {"ex1" map?
                "ex2" map?
                "ex3" map?}))

^{:refer spirit.data.exchange/delete-exchange :added "0.5"}
(fact "removes an exchange from the mq"

  (-> (exchange {:type :mock
                 :routing routes})
      (delete-exchange "ex1")
      (list-exchanges))
  => (contains {"ex2" map?}))

^{:refer spirit.data.exchange/list-bindings :added "0.5"}
(fact "returns current list of exchanges"

  (-> (exchange {:type :mock
                 :routing routes})
      (list-bindings))
  => (contains-in {"ex1" {:exchanges {"ex2" [map?]}
                          :queues {"q1" [map?]}}
                   "ex2" {:queues {"q2" [map?]}}}))

^{:refer spirit.data.exchange/bind-exchange :added "0.5"}
(fact "binds a queue to the exchange"

  (-> (exchange {:type :mock
                 :routing routes})
      (add-exchange "ex3")
      (bind-exchange "ex1" "ex3")
      (list-bindings))
  => (contains-in {"ex1" {:exchanges {"ex2" [map?]
                                      "ex3" [map?]}
                          :queues {"q1" [map?]}}
                   "ex2" {:queues {"q2" [map?]}}}))

^{:refer spirit.data.exchange/bind-queue :added "0.5"}
(fact "binds an exchange to the exchange"

  (-> (exchange {:type :mock :routing routes})
      (add-queue "q3")
      (bind-queue "ex1" "q3")
      (list-bindings))
  => (contains-in {"ex1" {:exchanges {"ex2" [map?]}
                          :queues {"q1" [map?]
                                   "q3" [map?]}}
                   "ex2" {:queues {"q2" [map?]}}}))

(def consumers
  {"q1" {:hello {:sync true :function #(prn % :hello)}
         :world {:sync true :function #(prn % :world)}}
   "q2" {:foo {:sync true :function #(prn % :foo)}}})

^{:refer spirit.data.exchange/list-consumers :added "0.5"}
(fact "lists all the consumers for the mq"

  (-> (exchange {:type :mock
                 :routing routes
                 :consumers consumers})
      (list-consumers))
  => (contains-in {"q1" {:hello map?,
                         :world map?},
                   "q2" {:foo map?}}))

^{:refer spirit.data.exchange/add-consumer :added "0.5"}
(fact "adds a consumers to the m"

  (-> (exchange {:type :mock
                 :routing routes
                 :consumers consumers})
      (add-consumer "q2" {:id :bar
                          :sync true
                          :function prn})
      (list-consumers))
  => (contains-in {"q1" {:hello map?
                         :world map?}
                   "q2" {:foo map?
                         :bar map?}}))

^{:refer spirit.data.exchange/delete-consumer :added "0.5"}
(fact "deletes the consumer from the queue"
  
  (-> (exchange {:type :mock
                 :routing routes
                 :consumers consumers})
      (delete-consumer "q1" :hello)
      (list-consumers))
  => (contains-in {"q1" {:world map?}
                   "q2" {:foo map?}}))

^{:refer spirit.data.exchange/publish :added "0.5"}
(fact "publishes a message to an exchange"

  (def p (promise))
  
  (-> (exchange {:type :mock
                 :routing routes
                 :consumers {"q1" {:hello {:function #(deliver p %)}}}})
      (publish "ex1" "hello there"))
  
  @p => "hello there")


^{:refer spirit.data.exchange/create :added "0.5"}
(fact "creates a exchange that is component compatible"

  (exchange/create {:type :mock
                    :file {:path "test.edn"
                           :reset true}})
  ;;=> #exchange.mock<uninitiased>
  )

^{:refer spirit.data.exchange/exchange :added "0.5"}
(fact "creates an active exchange"

  (exchange {:type :mock
             :routing routes
             :file {:path "test.edn"
                    :reset true
                    :cleanup true}})
  ;;=> #exchange.mock
  ;;   {:queues #{"q1" "q2"},
  ;;    :exchanges #{"ex1" "ex2"},
  ;;    :bindings {"ex1" {:exchanges #{"ex2"},
  ;;                      :queues #{"q1"}},
  ;;               "ex2" {:queues #{"q2"},
  ;;                      :exchanges #{}}}}
  )
