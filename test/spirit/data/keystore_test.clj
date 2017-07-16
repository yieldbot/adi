(ns spirit.data.keystore-test
  (:use hara.test)
  (:require [spirit.data.keystore :refer :all :as keystore]
            [hara.component :as component])
  (:import spirit.data.keystore.base.MockKeystore))

^{:refer spirit.data.keystore/create :added "0.5"}
(fact "creates a keystore that is component compatible"

  (keystore/create {:type :mock
                    :file {:path "test.edn"
                           :reset true}})
  ;;=> #keystore.mock<uninitiased>
  )

^{:refer spirit.data.keystore/keystore :added "0.5"}
(fact "creates an active keystore"

  (keystore {:type :mock
             :initial {:hello :world}
             :file {:path "test.edn"
                    :reset true
                    :cleanup true}})
  ;;=> #keystore.mock{:hello :world}
  )

^{:refer spirit.data.keystore/put-in :added "0.5"}
(fact "adds data to the database"

  (-> (keystore)
      (put-in [:b] {:d 2})
      (put-in [:b] {:c 2})
      (peek-in))
  => {:b {:d 2, :c 2}})

^{:refer spirit.data.keystore/peek-in :added "0.5"}
(fact "accesses all or part of the database"

  (-> (keystore)
      (put-in {:a 1, :b {:c 2}})
      (peek-in [:b]))
  => {:c 2})

^{:refer spirit.data.keystore/keys-in :added "0.5"}
(fact "returns all keys in the particular level"

  (-> (keystore)
      (put-in {:a 1, :b {:c 2}})
      (keys-in)
      (sort))
  => [:a :b])

^{:refer spirit.data.keystore/drop-in :added "0.5"}
(fact "removes keys in the database"
  
  (-> (keystore)
      (put-in {:a 1, :b {:c 2}})
      (drop-in [:a])
      (peek-in))
  => {:b {:c 2}})

^{:refer spirit.data.keystore/set-in :added "0.5"}
(fact "adds data to the database. overwrites"

  (-> (keystore)
      (set-in [:b] {:d 2})
      (set-in [:b] {:c 2})
      (peek-in))
  => {:b {:c 2}})

^{:refer spirit.data.keystore/select-in :added "0.5"}
(fact "selects the key values corresponding to the query"
  (-> (keystore)
      (set-in [:data] {:a 1
                       :b 2
                       :c 3
                       :d 4})
      (select-in [:data] odd?))
  => [[:a 1] [:c 3]])

^{:refer spirit.data.keystore/batch-in :added "0.5"}
(fact "batch add and removal of data"

  (-> (keystore)
      (put-in {:data {:a 1
                      :b 2}
               :input {:c 3
                       :d 4}})
      (batch-in {:data {:x 1}
                 :input {:y 2}}
                [[:data :a] [:input :d]])
      (peek-in))
  => {:data {:b 2, :x 1}
      :input {:c 3, :y 2}})
