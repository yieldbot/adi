(ns spirit.network.common-test
  (:use hara.test)
  (:require [spirit.network.common :refer :all]
            [spirit.network.base.singleton :as singleton]
            [clojure.core.async :as async]))

^{:refer spirit.network.common/response :added "0.5"}
(fact "creates a response from map"

  (response {:type :on/id})
  ;;=> #response{:type :on/id}
  )

^{:refer spirit.network.common/response? :added "0.5"}
(fact "checks if the object is a response"

  (-> (response {:type :on/id})
      (response?))
  => true)

^{:refer spirit.network.common/read-value :added "0.5"}
(fact "reads a clojure data structure from string"

  (read-value "{:a 1 :b 2}" :edn)
  => {:a 1 :b 2})

^{:refer spirit.network.common/write-value :added "0.5"}
(fact "writes a clojure data structure to string"

  (write-value [1 2 {:a 3}] :edn)
  => "[1 2 {:a 3}]")

^{:refer spirit.network.common/pack :added "0.5"}
(fact "prepares the package for transport"

  (pack {:format :edn} {:type :on/id})
  => "{:type :on/id}")

^{:refer spirit.network.common/unpack :added "0.5"}
(fact "after transport, reads the package out from string"

  (unpack {:format :edn} "{:type :on/id}")
  => {:type :on/id})

^{:refer spirit.network.common/random-uuid :added "0.5"}
(fact "creates a random uuid as a string")

^{:refer spirit.network.common/now :added "0.5"}
(fact "creates a the current time is ms")

^{:refer spirit.network.common/wrap-request :added "0.5"}
(fact "creates a function that tracks the request"

  (defn shortcut [{:keys [pending]} {:keys [tag]}]
    (async/put! (get @pending tag) "hello"))

  ((wrap-request shortcut)
   {:pending (atom {})
    :return {:type :value}}
   {})
  => "hello")

^{:refer spirit.network.common/wrap-unpack :added "0.5"}
(fact "helper wrapper, uses unpack"

  ((wrap-unpack (fn [_ msg] msg))
   {:format :edn}
   "[1 2 3 4]")
  => [1 2 3 4]

  ((wrap-unpack (fn [_ msg] msg))
   {:format :edn}
   "[1 2 3 4")
  => {:type :error/read-value
      :code :error
      :status :error
      :input "[1 2 3 4"
      :error {:message nil
              :format :edn}})

^{:refer spirit.network.common/dead-fn :added "0.5"}  
(fact "default function for messages that haven't been processed")

^{:refer spirit.network.common/process-fn :added "0.5"}  
(fact "default function for processes that"

  (process-fn {:handlers {:on/id (fn [data] data)}}
              {:type :on/id
               :code :request
               :request "hello"})
  => "hello")

^{:refer spirit.network.common/return-fn :added "0.5"}
(fact "default function for returning responses"

  (async/<!! (return-fn {:pending (atom {"ABCD" (async/promise-chan)})}
                        {:tag "ABCD" :response "hello"}))
  => {:tag "ABCD", :response "hello"})

^{:refer spirit.network.common/receive-fn :added "0.5"}
(fact "default function for routing received messages"

  (receive-fn {:fn {:process process-fn}
               :handlers {:on/id (fn [data] data)}}
              {:type :on/id :code :push :push "hello"})
  => "hello")

^{:refer spirit.network.common/wrap-response :added "0.5"}
(fact "wrapper for processing messages of code `:request`"

  ((wrap-response (fn [conn package]
                    (response package)))
   {:fn {:reply (fn [_ package] package)}}
   {:type :on/id :code :request :request "hello"})
  => {:type :on/id, :code :response, :request "hello"})

^{:refer spirit.network.common/wrap-time-start :added "0.5"}
(fact "wrapper for adding start time"

  ((wrap-time-start (fn [_ package]
                       package)
                    [:overall])
   nil
   {:params {:time true}})
  => (just-in {:params {:time true},
               :time {:overall {:start number?}}}))

^{:refer spirit.network.common/wrap-time-end :added "0.5"}
(fact "wrapper for adding start time"

  ((wrap-time-end (fn [_ package]
                       package)
                    [:overall])
   nil
   {:params {:time true
             :metrics true}
    :time {:overall {:start 0}}})
  => (just-in {:params {:time true
                        :metrics true}
               :time {:overall {:start 0
                                :end number?}}
               :metrics {:overall number?}}))

^{:refer spirit.network.common/wrap-display :added "0.5"}
(fact "wrapper for adding start time"

  ((wrap-display (fn [_ package]
                   package))
   nil
   {:params  {:time false
              :metrics true}
    :time    {:overall {:start 0
                       :end 1000}}
    :metrics {:overall 1000}})
  => {:params {:time false
               :metrics true}
      :metrics {:overall 1000}})

^{:refer spirit.network.common/wrap-track :added "0.5"}
(fact "wrapper to append a record of the time, id and function"

  ((wrap-track (fn [_ package]
                 package)
               :sent)
   {:id "ABCD"}
   {:params {:track true}})
  => (contains-in {:params {:track true},
                   :track [[:sent "ABCD" number?]]}))

^{:refer spirit.network.common/init-functions :added "0.5"}
(fact "helper function to create the network functions"

  (init-functions {:fn {:send   (fn [conn package] package)
                        :attach (fn [conn] conn)}})
  => (just-in {:id string?
               :pending #(instance? clojure.lang.Atom %)
               :fn {:active? fn?
                    :attach  fn?
                    :close   fn?
                    :dead    fn?
                    :process fn?
                    :receive fn?
                    :reply   fn?
                    :request fn?
                    :respond fn?
                    :return  fn?
                    :send    fn?}}))

^{:refer spirit.network.common/request :added "0.5"}
(fact "helper function for sending out a request"

  (def network
    (singleton/singleton
     {:id "A"
      :format :edn
      :options {:time  true
                :track true
                :network {:delay 100}}
      :default {:params {:full true
                         :metrics true}}
      :return  {:type    :channel
                :timeout 1000}
      :flags     {:on/id :full}
      :handlers  {:on/id (fn [req] (Thread/sleep 10) (:request req))}}))
  
  (request network :on/id :hello)
  => (just-in {:type :on/id,
               :code :response,
               :status :success
               :request :hello,
               :params {:full true,
                        :metrics true},
               :tag string?
               :response :hello,
               :metrics {:remote number?, :overall number?}}))

^{:refer spirit.network.common/message :added "0.5"}
(fact "helper function for messaging"

  (message network :on/id :hello)

  (Thread/sleep 500)
  
  (read-string (deref (:raw network)))
  => {:type :on/id
      :params {:full true, :metrics true}
      :code :data,
      :data :hello})
