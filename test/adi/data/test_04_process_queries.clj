(ns adi.data.test-04-process-queries
  (:use midje.sweet
        adi.utils
        adi.checkers)
  (:require [adi.data :as ad]))

(fact 
  (ad/process {:name [{:type :string}]}
              {:#/not     {:name "chris"}
               :#/not-any {:name #{"chris" "adam"}}})
  => {:# {:not {:name "chris"}
          :not-any {:name #{"chris" "adam"}}}})