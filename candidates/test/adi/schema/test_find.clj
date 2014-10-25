(ns adi.schema.test-find
  (:use midje.sweet
        adi.schema.find)
  (:require [adi.schema.find :as t]))

(fact "find-keys"
    (find-keys {:name [{:type :string}]} :type :string)
    => #{:name}

    (find-keys {:name [{:type :string}]} :type :other)
    => #{}

    (find-keys {:name [{:type :ref
                        :ref {:type :forward}}]}
               :ref (fn [r] (= :forward (:type r))))
    => #{:name}

    (find-keys {:name [{:type :ref
                        :ref {:type :forward}}]}
               (constantly true)
               :type :ref :ref (fn [r] (= :forward (:type r))))
    => #{:name})
