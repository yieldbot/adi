(ns spirit.data.cache.base
  (:require [spirit.protocol.icache :as cache]
            [spirit.common.atom :as atom]
            [hara.component :as component]))

(def ^:dynamic *current* nil)

(defn current
  "returns the current time that can be specified for testing
 
   (current)
   ;;=> 1500088974499
 
   (binding [*current* 1000]
     (current))
   => 1000"
  {:added "0.5"}
  ([] (current *current*))
  ([curr]
   (or curr (System/currentTimeMillis))))

(defn set-atom
  "sets the value of an atom
   (-> (atom {})
       (set-atom :hello :world)
       deref)
   => {:hello {:value :world}}
 
   (binding [*current* 1000]
     (-> (atom {})
         (set-atom :hello :world 1000)
         deref))
   => {:hello {:value :world,
               :expiration 1001000}}"
  {:added "0.5"}
  ([atom key value]
   (doto atom
     (swap! assoc key {:value value})))
  ([atom key value expiry]
   (doto atom
     (swap! assoc key {:value value
                       :expiration (+ (current)
                                      (* expiry 1000))}))))

(defn get-atom
  "sets the value of an atom
   (-> (atom {:hello {:value :world}})
       (get-atom :hello))
   => :world"
  {:added "0.5"}
  [atom key]
  (let [{:keys [value expiration]} (get @atom key)]
      (if (or (nil? expiration)
              (< (current) expiration))
        value)))

(defn all-atom
  "returns all valid entries in the atom
 
   (binding [*current* 1000]
     (-> (atom {:a {:value 1
                    :expiration 1001}
                :b {:value 2}
                :c {:value 3
                    :expiration 1000}})
        (all-atom)))
   => {:a 1, :b 2}"
  {:added "0.5"}
  [atom]
  (let [current (current)]
    (reduce-kv (fn [m k {:keys [value expiration]}]
                 (if (or (nil? expiration)
                         (< current expiration))
                   (assoc m k value)
                   m))
               {}
               @atom)))

(defn keys-atom
  "returns all valid entries in the atom
 
   (binding [*current* 1000]
     (-> (atom {:a {:value 1
                    :expiration 1001}
                :b {:value 2}
                :c {:value 3
                    :expiration 1000}})
        (keys-atom)
         sort))
   => [:a :b]"
  {:added "0.5"}
  [atom]
  (keys (all-atom atom)))

(defn count-atom
  "returns all valid entries in the atom
 
   (binding [*current* 1000]
     (-> (atom {:a {:value 1
                    :expiration 1001}
                :b {:value 2}
                :c {:value 3
                    :expiration 1000}})
        (count-atom)))
   => 2"
  {:added "0.5"}
  [atom]
  (count (keys-atom atom)))

(defn delete-atom
  "returns all valid entries in the atom
 
   (-> (atom {:a {:value 1}
              :b {:value 2}})
       (delete-atom :b)
       deref)
   => {:a {:value 1}}"
  {:added "0.5"}
  [atom key]
  (doto atom (swap! dissoc key)))

(defn batch-atom
  "creates a batched operation of inserts and deletes
   
   (binding [*current* 1000]
     (-> (atom {:a {:value 1}
                :b {:value 2}})
         (batch-atom {:c 3 :d 4}
                     {:c 10}
                     [:a])
         deref))
   => {:b {:value 2},
      :c {:value 3, :expiration 11000},
       :d {:value 4}}"
  {:added "0.5"}
  [atom add-values add-expiry remove-vec]
  (let [current (current)
        add-map (reduce-kv (fn [out k v]
                             (if-let [exp (get add-expiry k)]
                               (assoc out k {:value v :expiration (+ current
                                                                     (* exp 1000))})
                               (assoc out k {:value v})))
                           {}
                           add-values)]
    (doto atom
      (swap! #(apply dissoc
                     (merge % add-map)
                     remove-vec)))))

(defn clear-atom
  "resets the atom to an empty state
 
   (-> (atom {:a {:value 1}
              :b {:value 2}})
       (clear-atom)
       deref)
   => {}"
  {:added "0.5"}
  [atom]
  (doto atom (swap! empty)))

(defn touch-atom
  "extend expiration time if avaliable
 
   (binding [*current* 1000]
     (-> (atom {:a {:value 1
                    :expiration 1001}
                :b {:value 2}})
         (touch-atom :a 10)
         (touch-atom :b 10)
        deref))
   => {:a {:value 1, :expiration 11000}
       :b {:value 2}}"
  {:added "0.5"}
  [atom key expiry]
  (doto atom
    (swap! (fn [m] (if-let [{:keys [expiration]} (get m key)]
                     (if (and expiration
                              (< (current) expiration))
                       (assoc-in m [key :expiration]
                                 (+ (current) (* expiry 1000)))
                       m))))))

(defn expired?-atom
  "checks if key is expired
 
   (binding [*current* 1000]
     (let [atom (atom {:a {:value 1
                           :expiration 999}
                       :b {:value 2}
                       :c {:value 1
                           :expiration 1001}})]
      [(expired?-atom atom :a)
        (expired?-atom atom :b)
        (expired?-atom atom :c)]))
   => [true false false]"
  {:added "0.5"}
  [atom key]
  (if-let [{:keys [expiration]} (get @atom key)]
    (if (nil? expiration)
      false
      (< expiration (current)))))

(defn expiry-atom
  "checks if key is expired
 
   (binding [*current* 1000]
     (let [atom (atom {:a {:value 1
                           :expiration 999}
                       :b {:value 2}
                       :c {:value 1
                           :expiration 8000}})]
      [(expiry-atom atom :a)
        (expiry-atom atom :b)
        (expiry-atom atom :c)]))
   => [:expired :never 7]"
  {:added "0.5"}
  [atom key]
  (if-let [{:keys [expiration]} (get @atom key)]
    (cond (nil? expiration)
          :never
          
          (< expiration (current))
          :expired
          
          :else
          (quot (- expiration (current)) 1000))))

(extend-protocol cache/ICache
  
  clojure.lang.Atom
  (-set
    ([atom key value] (set-atom atom key value))
    ([atom key value expiry] (set-atom atom key value expiry)))

  (-get      [atom key] (get-atom atom key))
  (-count    [atom] (count-atom atom))
  (-batch    [atom add-values add-expiry remove-vec] (batch-atom atom add-values add-expiry remove-vec))
  (-delete   [atom key] (delete-atom atom key))
  (-clear    [atom] (clear-atom atom))
  (-all      [atom] (all-atom atom))
  (-keys     [atom] (keys-atom atom))
  (-touch    [atom key expiry] (touch-atom atom key expiry))
  (-expired? [atom key] (expired?-atom atom key))
  (-expiry   [atom key] (expiry-atom atom key)))

(defrecord MockCache [state]

  Object
  (toString [{:keys [state show] :as cache}]
    (str "#cache.mock"
         (if state
           (case show
             :keys (vec (cache/-keys cache))
             (cache/-all state))
           "<uninitiased>")))
  
  cache/ICache
  (-all    [cache] (cache/-all state))
  (-count [cache] (cache/-count state))
  (-expired? [cache key] (cache/-expired? state key))
  (-expiry [cache key] (cache/-expiry state key))
  (-get [cache key] (cache/-get state key))
  (-keys   [cache] (cache/-keys state))
  
  (-batch [cache add-values add-expiry remove-vec]
    (cache/-batch state add-values add-expiry remove-vec)
    cache)
  (-clear  [cache]
    (cache/-clear state)
    cache)
  (-delete [cache key]
    (cache/-delete state key)
    cache)
  (-set    [cache key value]
    (cache/-set state key value)
    cache)
  (-set [cache key value expiry]
    (cache/-set state key value expiry)
    cache)
  (-touch [cache key expiry]
    (cache/-touch state key expiry)
    cache)

  component/IComponent
  (-start [cache]
    (atom/attach-state cache))

  (-stop [cache]
    (atom/detach-state cache)))

(defmethod print-method MockCache
  [v w]
  (.write w (str v)))

(defmethod cache/create :mock
  [m]
  (map->MockCache m))
