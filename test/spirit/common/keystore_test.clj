(ns spirit.common.keystore-test
  (:use hara.test)
  (:require [spirit.common.keystore :refer :all])
  (:import spirit.common.keystore.MockKeystore))

^{:refer spirit.common.keystore/create :added "0.5"}
(fact "creates a keystore"

  (create {:type :raw})

  (create {:type :mock
           :file "test.edn"}))

^{:refer spirit.common.keystore/put-in :added "0.5"}
(fact "adds data to the database"

  (-> (create {:type :mock})
      (put-in [:b] {:d 2})
      (put-in [:b] {:c 2})
      (peek-in))
  => {:b {:d 2, :c 2}})

^{:refer spirit.common.keystore/peek-in :added "0.5"}
(fact "accesses all or part of the database"

  (-> (create {:type :mock})
      (put-in {:a 1, :b {:c 2}})
      (peek-in [:b]))
  => {:c 2})

^{:refer spirit.common.keystore/keys-in :added "0.5"}
(fact "returns all keys in the particular level"

  (-> (create {:type :mock})
      (put-in {:a 1, :b {:c 2}})
      (keys-in)
      (sort))
  => [:a :b])

^{:refer spirit.common.keystore/drop-in :added "0.5"}
(fact "removes keys in the database"
  
  (-> (create {:type :mock})
      (put-in {:a 1, :b {:c 2}})
      (drop-in [:a])
      (peek-in))
  => {:b {:c 2}})

^{:refer spirit.common.keystore/set-in :added "0.5"}
(fact "adds data to the database. overwrites"

  (-> (create {:type :mock})
      (set-in [:b] {:d 2})
      (set-in [:b] {:c 2})
      (peek-in))
  => {:b {:c 2}})

^{:refer spirit.common.keystore/select-in :added "0.5"}
(fact "selects the key values corresponding to the query"
  (-> (create {:type :mock})
      (set-in [:data] {:a 1
                       :b 2
                       :c 3
                       :d 4})
      (select-in [:data] odd?))
  => [[:a 1] [:c 3]])

^{:refer spirit.common.keystore/mutate-in :added "0.5"}
(fact "batch add and removal of data"

  (-> (create {:type :mock})
      (put-in {:data {:a 1
                      :b 2}
               :input {:c 3
                       :d 4}})
      (mutate-in []
                 {:data {:x 1}
                  :input {:y 2}}
                 [[:data :a] [:input :d]])
      (peek-in))
  => {:data {:b 2, :x 1}
      :input {:c 3, :y 2}})

^{:refer spirit.common.keystore/match :added "0.5"}
(fact "matches data according to the query"

  (match 1 odd?) => true

  (match {:a 1} {:a odd?}) => true)


^{:refer spirit.common.keystore/keystore :added "0.5"}
(fact "creates a standalone keystore"

  (keystore {:type :mock})
  => MockKeystore

  (keystore {:type :raw})
  => clojure.lang.Atom)
