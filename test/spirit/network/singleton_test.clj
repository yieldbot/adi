(ns spirit.network.singleton-test
  (:use hara.test)
  (:require [spirit.network.singleton :refer :all]
            [spirit.network.common :as common]
            [hara.component :as component]
            [clojure.core.async :as async])
  (:refer-clojure :exclude [send]))

^{:refer spirit.network.singleton/send-fn :added "0.5"}
(fact "send via agent with simulated network delay"

  (def result (promise))
  
  (send-fn {:raw (doto (agent nil)
                   (add-watch :test (fn [_ _ _ v]
                                      (deliver result v))))
            :format :edn
            :options {:network {:delay 100}}}
           {:type :on/id})

  @result
  => "{:type :on/id}")

^{:refer spirit.network.singleton/attach-fn :added "0.5"}
(fact "attaches the `:receive` function to the singleton'"

  (def raw (agent nil))
  (def result (promise))
  
  (do (attach-fn {:raw raw
                  :fn  {:receive (fn [conn package]
                                   (deliver result package))}})
      (send-off raw (constantly {:type :on/id})))
  @result
  => {:type :on/id})

^{:refer spirit.network.singleton/singleton :added "0.5"}
(fact "creates a singleton for simulating network activity"

  (def network
    (singleton {:id "A"
                :default {:params {:full true
                                   :metrics true}}
                :format :edn
                
                :options {:time  true
                          :track true
                          :network {:delay 100}}
                :return  {:type    :channel
                          :timeout 1000}
                :flags     {:on/id :full}
                :handlers  {:on/id (fn [req] (:request req))}})))

(fact "test for send via transport"
  
  (do ((-> network :fn :send) network {:type :on/id})
      (Thread/sleep 200)
      @(:raw network))
  => "{:type :on/id}"

  (do ((-> network :fn :send) network {:type :on/id
                                       :params {:track true}})
      (Thread/sleep 200)
      (read-string @(:raw network)))
  => (just-in {:type :on/id,
               :params {:track true},
               :track [[:send "A" number?]]}))

(fact "test for request via transport"
  
  (do ((-> network :fn :request) network {:type :on/id})
      (Thread/sleep 500)
      (read-string @(:raw network)))
  => (just-in {:type :on/id,
               :code :response,
               :tag string?
               :response nil})

  (do ((-> network :fn :request) network {:type :on/id
                                          :params {:metrics true
                                                   :track true}})
      (Thread/sleep 500)
      (read-string @(:raw network)))
  => (just-in {:type :on/id,
               :params {:metrics true, :track true},
               :code :response,
               :tag string?,
               :time {:overall {:start number?},
                      :remote  {:start number?
                                :end number?}},
               :track [[:send "A" number?]
                       [:receive "A" number?]
                       [:send "A" number?]],
               :response nil
               :metrics {:remote number?}}))

(fact "test for common/request"

  (common/request network :on/id "hello" )
  => (just-in {:type :on/id,
               :code :response,
               :params {:full true, :metrics true},
               :request "hello",
               :tag string?
               :response "hello",
               :metrics {:remote number?, :overall number?}}))
