(ns spirit.test.checkers
  (:use hara.test))
   
(defmacro raises-issue [payload]
  `(throws (fn [e#]
             ((contains ~payload) (ex-data e#)))))