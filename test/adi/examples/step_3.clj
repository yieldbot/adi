(ns adi.examples.step-3
  (:use midje.sweet)
  (:require [adi.core :as adi]
            [adi.test.checkers :refer :all]
            [datomic.api :as datomic]))

[[:section {:title "Step 3 - Refs and Entities"}]]

(def schema-3
 {:account {:user     [{:required true
                        :unique :value}]
            :password [{:required true
                        :restrict ["password needs an integer to be in the string"
                                   #(re-find #"\d" %)]}]
            :type     [{:type :enum
                        :default :free
                        :enum {:ns :account.type
                               :values #{:admin :free :paid}}}]
            :credits  [{:type :long
                        :default 0}]
            :books    [{:type :ref
                        :cardinality :many
                        :ref  {:ns :book}}]}
  :book   {:name    [{:required true
                      :fulltext true}]
           :author  [{:fulltext true}]}})


(def ds (adi/connect! "datomic:mem://adi-example-step-3" schema-3 true true))

(adi/insert! ds {:db/id [[:hello]] :account {:user "adi1" :password "hello1"}})
(adi/insert! ds {:account {:user "adi2" :password "hello2"
                           :books #{{:name "The Count of Monte Cristo"
                                     :author "Alexander Dumas"}
                                    {:name "Tom Sawyer"
                                     :author "Mark Twain"}
                                    {:name "Les Misérables"
                                     :author "Victor Hugo"}}}})

(fact "Select with :access model"
  (adi/select ds :account)
  => #{{:account {:user "adi1" :password "hello1" :credits 0 :type :free}}
       {:account {:user "adi2" :password "hello2" :credits 0 :type :free}}}

  (adi/select ds :account :access {:account {:books :checked}})
  => #{{:account {:credits 0 :type :free :password "hello1" :user "adi1"}}
       {:account {:books #{{:author "Mark Twain" :name "Tom Sawyer"}
                           {:author "Victor Hugo" :name "Les Misérables"}
                           {:author "Alexander Dumas" :name "The Count of Monte Cristo"}}
                  :credits 0 :type :free :password "hello2" :user "adi2"}}}

  (adi/select ds :account :access {:account {:books {:author :unchecked}
                                             :credits :unchecked
                                             :type :unchecked}})
  => #{{:account {:user "adi1" :password "hello1"}}
       {:account {:user "adi2" :password "hello2"
                  :books #{{:name "Tom Sawyer"}
                           {:name "The Count of Monte Cristo"}
                           {:name "Les Misérables"}}}}})

(fact "Adding ids"
  (let [account-ids (adi/select ds :account :return-ids)]
    (adi/insert! ds [{:book {:name "The Book and the Sword"
                             :author "Louis Cha"
                             :accounts account-ids}}]))

  (adi/select ds {:book {:accounts/user "adi1"}} :return {:book {:author :unchecked}})
  => #{{:book {:name "The Book and the Sword"}}}

  (adi/select ds {:book {:accounts/user "adi1"}}
              :access {:book {:accounts :checked}}
              :return {:book {:author :unchecked}})
  => #{{:book {:name "The Book and the Sword"}}})

(fact "Queries across references"
  (adi/insert! ds
               {:book {:name "Charlie and the Chocolate Factory"
                       :author "Roald Dahl"
                       :accounts #{{:user "adi3" :password "hello3" :credits 100}
                                   {:user "adi4" :password "hello4" :credits 500}
                                   {:user "adi5" :password "hello5" :credits 500}}}})

  (adi/select ds {:book/author '(?fulltext "Louis")} :return {:book {:accounts :checked}} :first)
  => {:book {:author "Louis Cha" :name "The Book and the Sword"
             :accounts #{{:credits 0 :type :free :password "hello2" :user "adi2"}
                         {:credits 0 :type :free :password "hello1" :user "adi1"}}}}

  (adi/select ds {:account {:books/name '(?fulltext "Charlie")}})
  => #{{:account {:user "adi3", :password "hello3", :credits 100, :type :free}}
       {:account {:user "adi4", :password "hello4", :credits 500, :type :free}}
       {:account {:user "adi5", :password "hello5", :credits 500, :type :free}}})

(fact "reflection stuff"
  (adi/select ds {:account {:books/name '(.contains ? "the")}}
              :return {:account {:password :unchecked
                                 :type     :unchecked
                                 :credits  :unchecked}})
  => #{{:account {:user "adi1"}} {:account {:user "adi2"}}
       {:account {:user "adi3"}} {:account {:user "adi4"}}
       {:account {:user "adi5"}}})
