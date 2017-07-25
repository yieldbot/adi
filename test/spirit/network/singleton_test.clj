(ns spirit.network.singleton-test
  (:use hara.test)
  (:require [spirit.network.singleton :refer :all]
            [hara.component :as component]
            [clojure.core.async :as async])
  (:refer-clojure :exclude [send]))

(fact "test for send and receive via transport"

  (def network (singleton {:id "A"
                           :default {:params {:time true}}
                           :format :edn
                           
                           :options {:time  true
                                     :track true
                                     :network {:delay 100}}
                           :return  {:type    :channel
                                     :timeout 1000}
                           :handlers {:on/id (fn [req] (Thread/sleep 10) (str req))}}))
  
  ((-> network :fn :send) network {:type :on/id})
  (def ch ((-> network :fn :request) network {:type :on/id}))
  (time (async/<!! ((-> network :fn :request) network {:type :on/id
                                                       :params {:time true}
                                                       })))
  
  (request network :on/id :hello)
  (send network :on/id :hello)
  
   
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
