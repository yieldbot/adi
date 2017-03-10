(ns adi.test.checkers
  (:use midje.sweet))
   
(defmacro raises-issue [payload]
  `(throws (fn [e#]
             ((contains ~payload) (ex-data e#)))))