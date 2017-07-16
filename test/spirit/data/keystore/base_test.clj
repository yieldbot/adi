(ns spirit.data.keystore.base-test
  (:use hara.test)
  (:require [spirit.data.keystore.base :refer :all]
            [spirit.protocol.ikeystore :as keystore]
            [hara.component :as component]))

^{:refer spirit.data.keystore.base/match :added "0.5"}
(fact "matches data according to the query"

  (match 1 odd?) => true

  (match {:a 1} {:a odd?}) => true)

^{:refer spirit.data.keystore.base/put-in-atom :added "0.5"}
(fact "puts a value in the atom"

  (-> (atom {:a {:b 1}})
      (put-in-atom [:a] {:c 2})
      deref)
  => {:a {:b 1 :c 2}})

^{:refer spirit.data.keystore.base/peek-in-atom :added "0.5"}
(fact "looks at the value in the atom"

  (-> (atom {:a {:b :c}})
      (peek-in-atom [:a]))
  => {:b :c})

^{:refer spirit.data.keystore.base/keys-in-atom :added "0.5"}
(fact "returns keys in the atom"

  (-> (atom {:a {:b 1 :c 2}})
      (keys-in-atom [:a]))
  => [:b :c])

^{:refer spirit.data.keystore.base/drop-in-atom :added "0.5"}
(fact "drops elements in the atom"

  (-> (atom {:a {:b 1 :c 2}})
      (drop-in-atom [:a :b])
      deref)
  => {:a {:c 2}})

^{:refer spirit.data.keystore.base/set-in-atom :added "0.5"}
(fact "drops elements in the atom"

  (-> (atom {:a {:b 1}})
      (set-in-atom [:a] {:c 2})
      deref)
  => {:a {:c 2}})

^{:refer spirit.data.keystore.base/select-in-atom :added "0.5"}
(fact "selects the necessary data in the data"

  (-> (atom {:a {:b 1 :c 2}})
      (select-in-atom [:a] even?))
  => [[:c 2]])

^{:refer spirit.data.keystore.base/batch-in-atom :added "0.5"}
(fact "perform batch add and remove operations"
  (-> (atom {:a {:b 1 :c 2}})
      (batch-in-atom [:a]
                     {:d 3 :e 4}
                     [[:b] [:c]]))
  => {:a {:d 3, :e 4}})

^{:refer spirit.data.keystore.base/keystore-file-out :added "0.5"}
(fact "test for file output"

  (-> (keystore/create {:type :mock
                        :file {:path "test.edn"
                               :reset true}})
      (component/start)
      (keystore/-put-in [:a :b] 1)
      (keystore/-put-in [:a :c] 2)
      (keystore/-put-in [:a :d] 3)
      (keystore/-peek-in [:a]))
  => {:b 1 :c 2 :d 3}

  (read-string (slurp "test.edn"))
  => {:a {:b 1 :c 2 :d 3}})


^{:refer spirit.data.keystore.base/keystore-log-out :added "0.5"}
(comment "test for file output"

  (with-out-str (-> (keystore/create {:type :mock
                                      :log true})
                    (component/start)
                    (keystore/-put-in [:a :b] 1)
                    (keystore/-put-in [:a :c] 2)
                    (keystore/-put-in [:a :d] 3)))
  => "LOG:\n{:a {:b 1}}\nLOG:\n{:a {:b 1, :c 2}}\nLOG:\n{:a {:b 1, :c 2, :d 3}}\n")
