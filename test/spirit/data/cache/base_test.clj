(ns spirit.data.cache.base-test
  (:use hara.test)
  (:require [spirit.data.cache.base :refer :all]
            [spirit.protocol.icache :as cache]
            [hara.component :as component]))

^{:refer spirit.data.cache.base/current :added "0.5"}
(fact "returns the current time that can be specified for testing"

  (current)
  ;;=> 1500088974499

  (binding [*current* 1000]
    (current))
  => 1000)

^{:refer spirit.data.cache.base/set-atom :added "0.5"}
(fact "sets the value of an atom"
  (-> (atom {})
      (set-atom :hello :world)
      deref)
  => {:hello {:value :world}}

  (binding [*current* 1000]
    (-> (atom {})
        (set-atom :hello :world 1000)
        deref))
  => {:hello {:value :world,
              :expiration 1001000}})

^{:refer spirit.data.cache.base/get-atom :added "0.5"}
(fact "sets the value of an atom"
  (-> (atom {:hello {:value :world}})
      (get-atom :hello))
  => :world)

^{:refer spirit.data.cache.base/all-atom :added "0.5"}
(fact "returns all valid entries in the atom"

  (binding [*current* 1000]
    (-> (atom {:a {:value 1
                   :expiration 1001}
               :b {:value 2}
               :c {:value 3
                   :expiration 1000}})
        (all-atom)))
  => {:a 1, :b 2})

^{:refer spirit.data.cache.base/keys-atom :added "0.5"}
(fact "returns all valid entries in the atom"

  (binding [*current* 1000]
    (-> (atom {:a {:value 1
                   :expiration 1001}
               :b {:value 2}
               :c {:value 3
                   :expiration 1000}})
        (keys-atom)
        sort))
  => [:a :b])

^{:refer spirit.data.cache.base/count-atom :added "0.5"}
(fact "returns all valid entries in the atom"

  (binding [*current* 1000]
    (-> (atom {:a {:value 1
                   :expiration 1001}
               :b {:value 2}
               :c {:value 3
                   :expiration 1000}})
        (count-atom)))
  => 2)

^{:refer spirit.data.cache.base/delete-atom :added "0.5"}
(fact "returns all valid entries in the atom"

  (-> (atom {:a {:value 1}
             :b {:value 2}})
      (delete-atom :b)
      deref)
  => {:a {:value 1}})

^{:refer spirit.data.cache.base/batch-atom :added "0.5"}
(fact "creates a batched operation of inserts and deletes"
  
  (binding [*current* 1000]
    (-> (atom {:a {:value 1}
               :b {:value 2}})
        (batch-atom {:c 3 :d 4}
                    {:c 10}
                    [:a])
        deref))
  => {:b {:value 2},
      :c {:value 3, :expiration 11000},
      :d {:value 4}})

^{:refer spirit.data.cache.base/clear-atom :added "0.5"}
(fact "resets the atom to an empty state"

  (-> (atom {:a {:value 1}
             :b {:value 2}})
      (clear-atom)
      deref)
  => {})

^{:refer spirit.data.cache.base/touch-atom :added "0.5"}
(fact "extend expiration time if avaliable"

  (binding [*current* 1000]
    (-> (atom {:a {:value 1
                   :expiration 1001}
               :b {:value 2}})
        (touch-atom :a 10)
        (touch-atom :b 10)
        deref))
  => {:a {:value 1, :expiration 11000}
      :b {:value 2}})

^{:refer spirit.data.cache.base/expired?-atom :added "0.5"}
(fact "checks if key is expired"

  (binding [*current* 1000]
    (let [atom (atom {:a {:value 1
                          :expiration 999}
                      :b {:value 2}
                      :c {:value 1
                          :expiration 1001}})]
      [(expired?-atom atom :a)
       (expired?-atom atom :b)
       (expired?-atom atom :c)]))
  => [true false false])

^{:refer spirit.data.cache.base/expiry-atom :added "0.5"}
(fact "checks if key is expired"

  (binding [*current* 1000]
    (let [atom (atom {:a {:value 1
                          :expiration 999}
                      :b {:value 2}
                      :c {:value 1
                          :expiration 8000}})]
      [(expiry-atom atom :a)
       (expiry-atom atom :b)
       (expiry-atom atom :c)]))
  => [:expired :never 7])
