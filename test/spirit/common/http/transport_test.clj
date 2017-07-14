(ns spirit.http.transport-test
  (:use hara.test)
  (:require [spirit.http.transport :refer :all]))

^{:refer spirit.http.transport/write-value :added "0.5"}
(fact "writes the string value of the datastructure according to format"

  (write-value {:a 1} :edn)
  => "{:a 1}")

^{:refer spirit.http.transport/read-value :added "0.5"}
(fact "read the string value of the datastructure according to format"

  (read-value "{:a 1}" :edn)
  => {:a 1})

^{:refer spirit.http.transport/read-body :added "0.5"}
(fact "reads the body of the request can be expanded"

  (read-body "{:a 1}" :edn)
  => {:a 1})

^{:refer spirit.http.transport/wrap-handler :added "0.5"}
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

^{:refer spirit.http.transport/response :added "0.5"}
(fact "constructs a Response object"

  (response {:id :on/info
             :header {:token "123password"}
             :data {:name "Chris"}})
  => spirit.http.transport.Response)

^{:refer spirit.http.transport/response? :added "0.5"}
(fact "checks if data is a response")
