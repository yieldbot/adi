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

  (coerce "2017-05-25T17:29:46.000Z" :instant)
  => (Date. 117 4 25 17 29 46)

  (coerce "2017-05-25T17:29:46Z" :instant)
  => (Date. 117 4 25 17 29 46)

  (coerce "oeuoe" :keyword)
  => :oeuoe)
