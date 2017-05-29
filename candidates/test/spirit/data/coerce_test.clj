(ns spirit.data.coerce-test
  (:use hara.test)
  (:require [spirit.data.coerce :refer :all]))

^{:refer spirit.data.coerce/assoc-set :added "0.3"}
(fact "associates a set as keys to a map"
  (assoc-set {} #{:a :b :c} 1)
  => {:a 1, :b 1, :c 1})


^{:refer spirit.data.coerce/coerce :added "0.3"}
(fact "associates a set as keys to a map"
  (coerce 1 :string)
  => "1"

  (coerce "oeuoe" :keyword)
  => :oeuoe)
