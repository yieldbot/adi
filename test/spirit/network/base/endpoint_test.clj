(ns spirit.network.base.endpoint-test
  (:use hara.test)
  (:require [spirit.network.base.endpoint :refer :all]
            [spirit.network.common :as common]
            [hara.component :as component]
            [clojure.core.async :as async]))

^{:refer spirit.network.base.endpoint/send-fn :added "0.5"}
(fact "default send function for active endpoint"

  (def ch (async/promise-chan))
  
  (send-fn {:raw (atom {:out ch})} {:hello :there})

  (async/<!! ch)
  => "{:hello :there}")

^{:refer spirit.network.base.endpoint/attach-fn :added "0.5"}
(fact "default attach function for active endpoint"

  (def result (promise))
  (def receive-fn (fn [conn package]
                    (deliver result package)))
  (def ch (async/chan))
  (def conn {:raw (atom {:in ch})
             :fn  {:receive receive-fn
                   :close close-fn}})
  
  (do (attach-fn conn)
      (async/put! ch {:hello :there})
      
      (deref result))
  => {:hello :there}
  
  (do (async/close! ch)
      (Thread/sleep 100)
      (deref (:raw conn)))
  => nil)

^{:refer spirit.network.base.endpoint/close-fn :added "0.5"}
(fact "default close function for active endpoint"

  (def conn {:raw (atom {:in  (async/chan)
                         :out (async/chan)})})

  (close-fn conn)
  (deref (:raw conn))
  => nil)

^{:refer spirit.network.base.endpoint/active?-fn :added "0.5"}
(fact "default check for active endpoint"

  (def conn {:raw (atom {:in  (async/chan)
                         :out (async/chan)})})

  (active?-fn conn)
  => true

  (close-fn conn)
  (active?-fn conn)
  => false)

^{:refer spirit.network.base.endpoint/endpoint :added "0.5"}
(fact "creates an endpoint"
  
  (def a (endpoint {:id "A"
                    :format   :edn
                    :options  {:time true :track true}
                    :default  {:params {:full true :metrics true}}
                    :return   {:type :channel :timeout 1000}
                    :handlers {:on/id (fn [req] (Thread/sleep 100) :a)}})))

^{:refer spirit.network.base.endpoint/connect :added "0.5"}
(fact "connects a pair of endpoints together"

  (def b (endpoint {:id "B"
                    :format   :edn
                    :options  {:time true :track true}
                    :default  {:params {:full true :metrics true}}
                    :return   {:type :channel :timeout 1000}
                    :handlers {:on/id (fn [req] (Thread/sleep 150) :b)}}))
  
  (connect a b)
  
  (common/request a :on/id {} {:params {:metrics true :full true}})
  => (contains-in {:type :on/id
                   :params {:metrics true
                            :full true}
                   :code :response
                   :request {}
                   :tag string?
                   :response :b
                   :metrics {:remote  number?
                             :overall number?}})


  (common/request a :on/oeuoeu {})
  => (contains {:type :on/oeuoeu
                :status :error
                :code :response}))
  
^{:refer spirit.network.base.endpoint/coupled :added "0.5"}
(fact "returns a pair of connected endpoints"
  
  (def duo (coupled {:id "A"
                     :format   :edn
                     :return   {:type :value}
                     :handlers {:on/id (fn [req] (Thread/sleep 100) :a)}}
                    {:id "B"
                     :format   :edn
                     :return   {:type :value}
                     :handlers {:on/id (fn [req] (Thread/sleep 100) :b)}}))
  
  (common/request (first duo) :on/id nil)
  => :b

  (common/request (second duo) :on/id nil)
  => :a)
