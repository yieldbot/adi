(ns adi.test-data
  (:use midje.sweet
        adi.utils)
  (:require [adi.data :as dt]))

(fact "trial keys will return the key sequence that gives the value of the map"
  (dt/trial-keys :a/b {:a/b 1}) => [:a/b]
  (dt/trial-keys :a/b {:+ {:a {:b 1}}}) => [:+ :a :b]
  (dt/trial-keys :a/b {:+ {:a/b 1}}) => [:+ :a/b]
  (dt/trial-keys :a/b {:a {:b 1}}) => [:a :b])

(def account-map
  (flatten-keys
   {:account {:username  [{:type   :string}]
              :password  [{:type   :string}]
              :tags      [{:type   :string
                           :cardinality :many}]}}))

(def account-res
  {:account/username "chris"
   :account/password "hello"})

(fact "process-data will produce the most basic data structure"
  (dt/process-data account-map
                 {:account {:username "chris"
                            :password "hello"}})
  => account-res

  (dt/process-data account-map
                 {:account/username "chris"
                  :account {:password "hello"}})
  => account-res

  (dt/process-data  account-map
                  {:+ {:account/username "chris"}
                   :account {:password "hello"}})
  => account-res

  (dt/process-data account-map
                 {:+ {:account {:username "chris"}}
                  :account/password "hello"})
  => account-res

  (dt/process-data account-map
                 {:account {:username 1
                            :password "hello"}})
  => (throws Exception)

  (dt/process-data account-map
                 {:account {:username nil
                            :password "hello"}})
  => {:account/password "hello"}

  (dt/process-data account-map
                 {:account/tags #{"fun" "happy" "still"}})
  => {:account/tags #{"fun" "happy" "still"}}

  (dt/process-data account-map
                 {:account/tags #{"fun" 3 "still"}})
  => (throws Exception)

  (dt/process-data account-map
                 {:account/tags ["fun" 3 "still"]})
  => (throws Exception))

(def link-map
  (flatten-keys
   {:link {:next  [{:type        :ref
                    :ref-ns      :link}]
           :value [{:type        :string
                    :default     "undefined"}]}}))

(def link-data
  {:link {:value "1"
          :next {:value "2"
                 :next  {:value "3"
                         :next {:value "4"}}}}})

(def link-circular
  {:db/id (dt/iid :start)
   :link {:value "1"
          :next {:value "2"
                 :next  {:value "3"
                         :next {:value "4"
                                :next {:+ {:db/id (dt/iid :start)}}}}}}})
(def link-circular2
  {:db/id (dt/iid :start)
   :link {:value "1"
          :next {:value "2"
                 :+ {:db/id (dt/iid :start)}
                 :next  {:value "3"
                         :next {:value "4"
                                :next {}}}}}})

(def link-res
  {:link/value "1"
   :link/next {:link/value "2"
               :link/next {:link/value "3"
                           :link/next {:link/value "4"}}}})


(fact "deprocess-data will reverse the effect of process data"
  (dt/deprocess-data account-map account-res)
  => {:account {:username "chris"
                :password "hello"}}

  (dt/deprocess-data link-map link-res)
  => link-data

  #_(dt/deprocess-data
   link-map
   (dt/process-data link-map link-circular))

  (dt/deprocess-data
   link-map
   (dt/process-data link-map link-circular2))
  => nil)

(fact "Different types of data links are allowed"
  (dt/process-data
   link-map
   link-data)
  => link-res

  (dt/process-data
   link-map
   {:link {:value "1"
           :next
           {:+ {:link {:value "2"
                       :next
                       {:+ {:link {:value "3"
                                   :next
                                   {:+ {:link {:value "4"}}}}}}}}}}})
  => link-res)

(fact "default values are only added for namespaces"
  (dt/process-data link-map {})
  => {}

  (dt/process-data
   link-map {:link {:next {}}})
  => {:link/value "undefined"
      :link/next {:link/value "undefined"}}

  (dt/process-data
   link-map {:link {:next {}}} false)
  => {:link/next {}})


(fact "characterise sorts out the different data structures"
  (dt/characterise link-map
                   (dt/process-data
                    link-map
                    {:db/id (dt/iid :start)
                     :link {:value "1"
                            :next {:value "2"
                                   :next  {:value "3"
                                           :next {:value "4"
                                                  :+ {:db/id (dt/iid :start)}}}}}}))


  => {:data-one {:link/value "1"}
      :ref-one
      {:link/next
       {:data-one {:link/value "2"},
        :ref-one  {:link/next
                   {:data-one {:link/value "3"}
                    :ref-one
                    {:link/next
                     {:data-one {:link/value "4"}}}}}}}})

(dt/generate-data link-map link-data)

(dt/generate-data link-map link-circular)
