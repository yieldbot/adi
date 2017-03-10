(ns adi.data.coerce-test
  (:use midje.sweet)
  (:require [adi.data.coerce :refer :all]))

^{:refer adi.data.coerce/assoc-set :added "0.3"}
(fact "associates a set as keys to a map"
  (assoc-set {} #{:a :b :c} 1)
  => {:a 1, :b 1, :c 1})


^{:refer adi.data.coerce/coerce :added "0.3"}
(fact "associates a set as keys to a map"
  (coerce 1 :string)
  => "1"

  (coerce "oeuoe" :keyword)
  => :oeuoe)
