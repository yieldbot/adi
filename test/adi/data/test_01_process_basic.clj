(ns adi.data.test-01-process-basic
  (:use midje.sweet
        adi.utils
        adi.checkers)
  (:require [adi.data :as ad]))

(fact "process basic usage"
  (ad/process {:name [{:type :string}]}
              {})
  => {}

  (ad/process {:name [{:type :string}]}
              {:name "chris"})
  => {:name "chris"}

  (ad/process {:name [{:type :string}]}
              {:other-value "something else"})
  => {}

  (ad/process {:name [{:type :string}]}
              {:name :wrong-type})
  => (throws Exception)

  (ad/process {:name [{:type :string}]}
              {:+ {:name "chris"}})
  => {:name "chris"}

  (ad/process {:name [{:type :string}]}
              {:name "chris"
               :#/not     {:name "chris"}
               :#/not-any [[:name "chris"]
                           [:name "adam"]] })
  => {:name "chris"
      :#/not {:name "chris"}
      :#/not-any [[:name "chris"]
                  [:name "adam"]]})


(def account-map
  (flatten-keys
   {:ac {:name  [{:type   :string}]
         :pass  [{:type   :string}]
         :tags  [{:type   :string
                  :cardinality :many}]}}))

(def account-res
  {:ac/name "chris" :ac/pass "hello"})


(fact "process will produces the most basic data structures"
  (ad/process account-map
                 {:ac {:name nil
                       :pass nil}})
  => {}

  (ad/process account-map
                 {:ac {:name nil
                       :pass "hello"}})
  => {:ac/pass "hello"}

  (ad/process account-map
                 {:ac {:name "chris"
                       :pass "hello"}})
  => account-res

  (ad/process account-map
                 {:ac/name "chris"
                  :ac {:pass "hello"}})
  => account-res

  (ad/process  account-map
                  {:+ {:ac/name "chris"}
                   :ac {:pass "hello"}})
  => account-res

  (ad/process account-map
                 {:+ {:ac {:name "chris"}}
                  :ac/pass "hello"})
  => account-res

  (ad/process account-map
              {:ac/tags #{"fun" "happy" "still"}})
  => {:ac/tags #{"fun" "happy" "still"}})

(fact "process exceptions. what makes it blow up"
  (ad/process account-map
                 {:ac {:name 1 ;; wrong data type
                       :pass "hello"}})
  => (throws Exception)

  (ad/process account-map
              {:ac/tags #{"fun" 3 "still"}}) ;; not all strings
  => (throws Exception)
  (ad/process account-map
              {:ac/tags ["fun" "happy" "still"]}) ;; not a set
  => (throws Exception) )


(def game-map
  (flatten-keys
   {:game {:name  [{:type    :string}]
           :score [{:type    :long
                    :default 0}]}}))
(fact "process exceptions. what makes it blow up"
  (ad/process game-map
                 {:game {:name "adam"}})
  => {:game/name "adam"
      :game/score 0})


