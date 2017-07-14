(ns spirit.transport.net-test
  (:use hara.test)
  (:require [spirit.transport.net :refer :all]))

^{:refer spirit.transport.net/write-value :added "0.5"}
(fact "writes the string value of the datastructure according to format"

  (write-value {:a 1} :edn)
  => "{:a 1}")

^{:refer spirit.transport.net/read-value :added "0.5"}
(fact "read the string value of the datastructure according to format"

  (read-value "{:a 1}" :edn)
  => {:a 1})

^{:refer spirit.transport.net/read-body :added "0.5"}
(fact "reads the body of the request can be expanded"

  (read-body "{:a 1}" :edn)
  => {:a 1})

^{:refer spirit.transport.net/wrap-handler :added "0.5"}
(fact "wraps a handler given a lookup and a config"

  ((wrap-handler identity
                  [:add-number
                   :mul-number]
                  {:ops {:add 5 :mul 10}}         
                  {:add-number {:func (fn [handler arg]
                                        (fn [number]
                                          (+ (handler number) arg)))
                                :args [[:ops :add]]}
                   :mul-number {:func (fn [handler arg]
                                        (fn [number]
                                          (* (handler number) arg)))
                                :args [[:ops :mul]]}})
   10)
  => 150)

^{:refer spirit.transport.net/response :added "0.5"}
(fact "constructs a Response object"

  (response {:id :on/info
             :header {:token "123password"}
             :data {:name "Chris"}})
  => spirit.transport.net.Response)

^{:refer spirit.transport.net/response? :added "0.5"}
(fact "checks if data is a response")
