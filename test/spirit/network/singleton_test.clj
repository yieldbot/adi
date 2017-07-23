(ns spirit.network.singleton-test
  (:use hara.test)
  (:require [spirit.network.singleton :refer :all]
            [hara.component :as component]))

(fact "test for send and receive via transport"

  (def network (singleton {:format :edn
                           :handler {:on/id (fn [req]
                                              (prn req)
                                              :on/id)}}))
  
  (:fn network)
  
  
  ((-> network :fn :send) network {:id :on/id})
  
  @(:raw network)
  
  (component/stop network)
  )
