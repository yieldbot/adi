(ns spirit.common.keys-test
  (:use hara.test)
  (:require [spirit.common.keys :refer :all]))
            
^{:refer spirit.common.keys/keyword-reverse :added "0.3"}
(fact "reverses the keyword by either adding or removing '_' in the value"
  (keyword-reverse :a/b) => :a/_b
  (keyword-reverse :a/_b) => :a/b
  (keyword-reverse :b) => (throws Exception)
  ^:hidden
  ;;(keyword-reverse :a/b/c) => :a/b/_c
  ;;(keyword-reverse :a/b/_c) => :a/b/c
  (keyword-reverse :_b) => (throws Exception))

^{:refer spirit.common.keys/keyword-reversed? :added "0.3"}
(fact "checks whether the keyword is reversed (begins with '_')"
  (keyword-reversed? :a) => false
  (keyword-reversed? :a/b) => false
  (keyword-reversed? :a/_b) => true
  ^:hidden
  ;;(keyword-reversed? :a/b/_c) => true
  (keyword-reversed? :_a) => false)
