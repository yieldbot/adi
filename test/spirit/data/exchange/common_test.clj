(ns spirit.data.exchange.common-test
  (:use hara.test)
  (:require [spirit.data.exchange.common :refer :all]
            [spirit.data.exchange.base :as base]
            [spirit.protocol.iexchange :as exchange]
            [hara.component :as component]))

(def routes {:queues    #{"q1" "q2"},
             :exchanges #{"ex1" "ex2"},
             :bindings   {"ex1" {:exchanges #{"ex2"},
                                 :queues #{"q1"}}
                          "ex2" {:exchanges #{}
                                 :queues #{"q2"}}}})

^{:refer spirit.data.exchange.common/lengthen-topology :added "0.5"}
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

^{:refer spirit.data.exchange.common/shorten-topology :added "0.5"}
(fact "creates a shorthand version of the routing topology"

  (shorten-topology (lengthen-topology routes))
  => routes)

^{:refer spirit.data.exchange.common/install-bindings :added "0.5"}
(fact "installs bindings on the mq"
  (-> (atom {})
      (exchange/-add-queue "q1" {})
      (exchange/-add-exchange "ex1" {})
      (install-bindings {"ex1" {:queues {"q1" [{}]}}})
      (exchange/-list-bindings))
  => (contains-in {"ex1" {:queues {"q1" [map?]}}}))

^{:refer spirit.data.exchange.common/routing :added "0.5"}
(fact "returns the routes for display"

  (-> (atom {})
      (routing {:short true}))
  => {:queues #{}, :exchanges #{}, :bindings {}})

^{:refer spirit.data.exchange.common/install-routing :added "0.5"}
(fact "installs routing on the mq"
  (-> (atom {})
      (install-routing routes)
      (routing {:short true}))
  => routes)

^{:refer spirit.data.exchange.common/remove-routing :added "0.5"}
(fact "removes routing on the mq"
  
  (-> (exchange/create {:type :mock
                        :routing routes})
      (component/start)
      (exchange/-add-queue "q3" {})
      (remove-routing)
      (routing {:short true}))
  => {:queues #{"q3"}, :exchanges #{}, :bindings {}})

^{:refer spirit.data.exchange.common/purge-routing :added "0.5"}
(fact "clears all routing on the mq"
  
  (-> (exchange/create {:type :mock
                        :routing routes})
      (component/start)
      (purge-routing)
      (routing {:short true}))
  => {:queues #{}, :exchanges #{}, :bindings {}})

^{:refer spirit.data.exchange.common/install-consumers :added "0.5"}
(fact "installs-consumers on the queues")
