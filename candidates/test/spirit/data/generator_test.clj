(ns spirit.data.generator-test
  (:use hara.test)
  (:require [spirit.data.generator :refer :all]))

^{:refer spirit.data.generator/incremental-sym-gen :added "0.3"}
(fact "constructs a function that generate incremental symbols 
  when repeatedly called."
  (repeatedly 5 (incremental-sym-gen 'e)) 
  => '(?e1 ?e2 ?e3 ?e4 ?e5))


^{:refer spirit.data.generator/incremental-id-gen :added "0.3"}
(fact "constructs a function that generate incremental ids 
  when repeatedly called."
  (repeatedly 5 (incremental-id-gen 100)) 
  => [101 102 103 104 105])
