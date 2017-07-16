(ns spirit.data.cache
  (:require [spirit.protocol.icache :as cache]
            [spirit.data.cache.base :as base]
            [hara.component :as component])
  (:refer-clojure :exclude [get set keys count]))

(defn set
  "sets the value with or optional expiry
 
   (-> (cache)
       (cache/set :a 1)
       :state
       deref)
   => {:a {:value 1}}"
  {:added "0.5"}
  ([cache key value]
   (cache/-set cache key value))
  ([cache key value expiry]
   (cache/-set cache key value expiry)))

(defn get
  "returns the value of a key
   
   (-> (cache {:type :mock
               :initial {:a {:value 1} :b {:value 2} :c {:value 3}}})
       (cache/get :a))
   => 1"
  {:added "0.5"}
  [cache key]
  (cache/-get cache key))

(defn count
  "returns number of active keys in the cache
 
   (-> (cache {:type :mock
               :initial {:a {:value 1} :b {:value 2}}})
       (cache/count))
   => 2"
  {:added "0.5"}
  [cache]
  (cache/-count cache))

(defn batch
  "performs multiple operations in the cache
 
   (-> (cache {:type :mock
               :initial {:a {:value 1} :b {:value 2}}})
       (cache/batch {:c 3 :d 4} {} [:b])
       (cache/all))
   => {:a 1, :c 3, :d 4}"
  {:added "0.5"}
  ([cache add-values]
   (cache/-batch cache add-values {} []))
  ([cache add-values add-expiry remove-vec]
   (cache/-batch cache add-values add-expiry remove-vec)))

(defn delete
  "deletes given key
 
   (-> (cache {:type :mock
               :initial {:a {:value 1} :b {:value 2}}})
       (cache/delete :b)
       (cache/all))
   => {:a 1}"
  {:added "0.5"}
  [cache key]
  (cache/-delete cache key))

(defn clear
  "clears the cache
 
   (-> (cache {:type :mock
               :initial {:a {:value 1} :b {:value 2}}})
       (cache/clear)
       (cache/all))
   => {}"
  {:added "0.5"}
  [cache]
  (cache/-clear cache))

(defn all
  "returns all values in the cache
   
   (-> (cache {:type :mock
               :initial {:a {:value 1} :b {:value 2}}})
       (cache/all))
   => {:a 1, :b 2}"
  {:added "0.5"}
  [cache]
  (cache/-all cache))

(defn keys
  "returns all keys in the cache
 
   (-> (cache {:type :mock
               :initial {:a {:value 1} :b {:value 2}}})
       (cache/keys)
       sort)
   => [:a :b]"
  {:added "0.5"}
  [cache]
  (cache/-keys cache))

(defn touch
  "renews the expiration time for a given key
   
   (binding [base/*current* 1000]
     (-> (cache {:type :mock
                 :initial {:a {:value 1 :expiration 1001}}})
         (cache/touch :a 10)
         :state
         deref))
   => {:a {:value 1, :expiration 11000}}"
  {:added "0.5"}
  [cache key expiry]
  (cache/-touch cache key expiry))

(defn expired?
  "checks if a given key time is expired
   
   (binding [base/*current* 1000]
     (-> (cache {:type :mock
                 :initial {:a {:value 1 :expiration 1001}}})
         (cache/expired? :a)))
   => false"
  {:added "0.5"}
  [cache key]
  (cache/-expired? cache key))

(defn expiry
  "return the expiry of a key in seconds
 
   (binding [base/*current* 1000]
     (-> (cache {:type :mock
                 :initial {:a {:value 1 :expiration 7000}}})
         (cache/expiry :a)))
   => 6"
  {:added "0.5"}
  [cache key]
  (cache/-expiry cache key))

(defn create
  "creates a cache that is component compatible
 
   (cache/create {:type :mock
                  :file {:path \"test.edn\"
                         :reset true}})
   ;;=> #cache.mock<uninitiased>
   "
  {:added "0.5"}
  [m]
  (cache/create m))

(defn cache
  "creates an active cache
 
   (cache {:type :mock
           :initial {:a {:value 1}}
           :file {:path \"test.edn\"
                  :reset true}})
   ;;=> #cache.mock{:a 1}
  "
  {:added "0.5"}
  ([] (cache {:type :mock}))
  ([m]
   (-> (cache/create m)
       (component/start))))
