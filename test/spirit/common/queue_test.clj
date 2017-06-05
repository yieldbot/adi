(ns spirit.common.queue-test
  (:use hara.test)
  (:require [spirit.common.queue :refer :all]))

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

  (def routes {:queues    #{"q1" "q2"},
               :exchanges #{"ex1" "ex2"},
               :bindings   {"ex1" {:exchanges #{"ex2"},
                                   :queues #{"q1"}}
                            "ex2" {:exchanges #{}
                                   :queues #{"q2"}}}})
  
  (shorten-topology (lengthen-topology routes))
  => routes)


