(ns spirit.data.cache-test
  (:use [hara.test :exclude [all]])
  (:require [spirit.data.cache :as cache :refer [cache]]
            [spirit.data.cache.base :as base]
            [hara.component :as component])
  (:import spirit.data.cache.base.MockCache))

^{:refer spirit.data.cache/set :added "0.5"}
(fact "sets the value with or optional expiry"

  (-> (cache)
      (cache/set :a 1)
      :state
      deref)
  => {:a {:value 1}})

^{:refer spirit.data.cache/get :added "0.5"}
(fact "returns the value of a key"
  
  (-> (cache {:type :mock
              :initial {:a {:value 1} :b {:value 2} :c {:value 3}}})
      (cache/get :a))
  => 1)

^{:refer spirit.data.cache/count :added "0.5"}
(fact "returns number of active keys in the cache"

  (-> (cache {:type :mock
              :initial {:a {:value 1} :b {:value 2}}})
      (cache/count))
  => 2)

^{:refer spirit.data.cache/batch :added "0.5"}
(fact "performs multiple operations in the cache"

  (-> (cache {:type :mock
              :initial {:a {:value 1} :b {:value 2}}})
      (cache/batch {:c 3 :d 4} {} [:b])
      (cache/all))
  => {:a 1, :c 3, :d 4})

^{:refer spirit.data.cache/delete :added "0.5"}
(fact "deletes given key"

  (-> (cache {:type :mock
              :initial {:a {:value 1} :b {:value 2}}})
      (cache/delete :b)
      (cache/all))
  => {:a 1})

^{:refer spirit.data.cache/clear :added "0.5"}
(fact "clears the cache"

  (-> (cache {:type :mock
              :initial {:a {:value 1} :b {:value 2}}})
      (cache/clear)
      (cache/all))
  => {})

^{:refer spirit.data.cache/all :added "0.5"}
(fact "returns all values in the cache"
  
  (-> (cache {:type :mock
              :initial {:a {:value 1} :b {:value 2}}})
      (cache/all))
  => {:a 1, :b 2})

^{:refer spirit.data.cache/keys :added "0.5"}
(fact "returns all keys in the cache"

  (-> (cache {:type :mock
              :initial {:a {:value 1} :b {:value 2}}})
      (cache/keys)
      sort)
  => [:a :b])

^{:refer spirit.data.cache/touch :added "0.5"}
(fact "renews the expiration time for a given key"
  
  (binding [base/*current* 1000]
    (-> (cache {:type :mock
                :initial {:a {:value 1 :expiration 1001}}})
        (cache/touch :a 10)
        :state
        deref))
  => {:a {:value 1, :expiration 11000}})

^{:refer spirit.data.cache/expired? :added "0.5"}
(fact "checks if a given key time is expired"
  
  (binding [base/*current* 1000]
    (-> (cache {:type :mock
                :initial {:a {:value 1 :expiration 1001}}})
        (cache/expired? :a)))
  => false)

^{:refer spirit.data.cache/expiry :added "0.5"}
(fact "return the expiry of a key in seconds"

  (binding [base/*current* 1000]
    (-> (cache {:type :mock
                :initial {:a {:value 1 :expiration 7000}}})
        (cache/expiry :a)))
  => 6)

^{:refer spirit.data.cache/create :added "0.5"}
(fact "creates a cache that is component compatible"

  (cache/create {:type :mock
                 :file {:path "test.edn"
                        :reset true}})
  ;;=> #cache.mock<uninitiased>
  )

^{:refer spirit.data.cache/cache :added "0.5"}
(fact "creates an active cache"

  (cache {:type :mock
          :initial {:a {:value 1}}
          :file {:path "test.edn"
                 :reset true}})
  ;;=> #cache.mock{:a 1}
  )
