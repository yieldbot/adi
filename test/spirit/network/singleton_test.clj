(ns spirit.network.singleton-test
  (:use hara.test)
  (:require [spirit.network.singleton :refer :all]
            [hara.component :as component]
            [clojure.core.async :as async]))

(fact "test for send and receive via transport"

  (def network (singleton {:format :edn
                           :options {:network {:delay 100}}
                           :return {:type :channel
                                    :timeout 100}
                           :handlers {:on/id (fn [req]
                                               (prn req)
                                               :on/id)}}))
  
  ((-> network :fn :send) network {:type :on/id})
  (def ch ((-> network :fn :request) network {:type :on/id}))
  (time (async/<!! ((-> network :fn :request) network {:type :on/id})))
  
  (:raw network)
  (:pending network)

  (comment
    (async/<!!
     (async/go (async/<! (async/alt! (async/go
                                       (async/timeout 1000)
                                       "hello")
                                     
                                     (async/chan)))))
    
    
    (async/<!! (async/timeout 1000))
    (component/stop network))
  )
