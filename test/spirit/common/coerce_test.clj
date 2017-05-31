(ns spirit.common.coerce-test
  (:use hara.test)
  (:require [spirit.common.coerce :refer :all]))

^{:refer spirit.common.coerce/assoc-set :added "0.3"}
(fact "associates a set as keys to a map"
  (assoc-set {} #{:a :b :c} 1)
  => {:a 1, :b 1, :c 1})


^{:refer spirit.common.coerce/coerce :added "0.3"}
(fact "associates a set as keys to a map"
  (coerce 1 :string)
  => "1"

  (coerce "oeuoe" :keyword)
  => :oeuoe)
