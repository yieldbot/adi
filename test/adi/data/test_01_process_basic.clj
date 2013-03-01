(ns adi.data.test-01-process-basic
  (:use midje.sweet
        adi.utils
        adi.checkers)
  (:require [adi.data :as ad]))

(fact "process basic usage"
  (ad/process {} {:name [{:type :string}]})
  => {}

  (ad/process {:name "chris"} {:name [{:type :string}]})
  => {:name "chris"}

  (ad/process {:other-value "something else"} {:name [{:type :string}]})
  => {}

  (ad/process {:name :wrong-type}
              {:name [{:type :string}]})
  => (throws Exception)

  (ad/process {:+ {:name "chris"}}
              {:name [{:type :string}]})
  => {:name "chris"}

  (ad/process {:name "chris"
               :#/not     {:name "chris"}
               :#/not-any [[:name "chris"]
                           [:name "adam"]] }
              {:name [{:type :string}]})
  => {:name "chris"
      :# {:not {:name "chris"}
          :not-any [[:name "chris"]
                    [:name "adam"]]}})


(def account-map
  (flatten-keys
   {:ac {:name  [{:type        :string}]
         :pass  [{:type        :string}]
         :tags  [{:type        :string
                  :cardinality :many}]}}))

(def account-res
  {:ac/name "chris" :ac/pass "hello"})


(fact "process will produces the most basic data structures"
  (ad/process {:ac {:name nil
                       :pass nil}}
                 account-map)
  => {}

  (ad/process {:ac {:name nil
                       :pass "hello"}}
                 account-map)
  => {:ac/pass "hello"}

  (ad/process {:ac {:name "chris"
                       :pass "hello"}}
                 account-map)
  => account-res

  (ad/process {:ac/name "chris"
                  :ac {:pass "hello"}}
                 account-map)
  => account-res

  (ad/process {:+ {:ac/name "chris"}
               :ac {:pass "hello"}}
              account-map)
  => account-res

  (ad/process {:+ {:ac {:name "chris"}}
               :ac/pass "hello"}
              account-map)
  => account-res

  (ad/process {:ac/tags #{"fun" "happy" "still"}} account-map)
  => {:ac/tags #{"fun" "happy" "still"}})

(fact "process exceptions. what makes it blow up"
  (ad/process {:ac {:name 1 ;; wrong data type
                       :pass "hello"}}
                 account-map)
  => (throws Exception)

  (ad/process {:ac/tags #{"fun" 3 "still"}}
              account-map) ;; not all strings
  => (throws Exception)
  (ad/process {:ac/tags ["fun" "happy" "still"]}
              account-map) ;; not a set
  => (throws Exception))


(def game-map
  (flatten-keys
   {:game {:name  [{:type    :string}]
           :score [{:type    :long
                    :default 0}]}
    :profile {:avatar [{:type    :keyword
                        :default :human}]}}))

(fact "checking defaults"
  (ad/process {} game-map)
  => {}

  (ad/process {:game {:name "adam"}}
              game-map)
  => {:game/name "adam"
      :game/score 0}

  (ad/process {:profile {} :game {}} game-map
              {:add-defaults? true
               :default-nss #{:profile :game}})
  => {:profile/avatar :human
      :game/score 0}

  (ad/process {:game {:name "adam"} :profile {}}
              game-map {:add-defaults? true
                        :default-nss #{:profile}})
  => {:game/name "adam"
      :profile/avatar :human})
