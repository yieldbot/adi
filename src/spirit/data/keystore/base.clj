(ns spirit.data.keystore.base
  (:require [spirit.protocol.ikeystore :as keystore]
            [spirit.common.atom :as atom]
            [hara.component :as component]
            [hara.data.nested :as nested]
            [hara.data.map :as map]
            [hara.io.file :as fs]))

(defn match
  "matches data according to the query
 
   (match 1 odd?) => true
 
   (match {:a 1} {:a odd?}) => true"
  {:added "0.5"}
  [data query]
  (cond (and (map? data)
             (map? query))
        (->> (keys query)
             (map (fn [k]
                    (let [sdata  (get data k)
                          squery (get query k)]
                      (if (nil? sdata)
                        false
                        (match sdata squery)))))
             (every? true?))

        (fn? query)
        (query data)

        :else
        (= query data)))

(defn put-in-atom
  "puts a value in the atom
 
   (-> (atom {:a {:b 1}})
       (put-in-atom [:a] {:c 2})
       deref)
   => {:a {:b 1 :c 2}}"
  {:added "0.5"}
  [atom path v]
  (let [im (if (empty? path) v
               (assoc-in {} path v))]
    (swap! atom nested/merge-nested im)
    atom))

(defn peek-in-atom
  "looks at the value in the atom
 
   (-> (atom {:a {:b :c}})
       (peek-in-atom [:a]))
   => {:b :c}"
  {:added "0.5"}
  [atom path]
  (if (empty? path)
    (or @atom {})
    (get-in @atom path)))

(defn keys-in-atom
  "returns keys in the atom
 
   (-> (atom {:a {:b 1 :c 2}})
       (keys-in-atom [:a]))
   => [:b :c]"
  {:added "0.5"}
  [atom path]
  (-> atom (peek-in-atom path) keys))
  
(defn drop-in-atom
  "drops elements in the atom
 
   (-> (atom {:a {:b 1 :c 2}})
       (drop-in-atom [:a :b])
       deref)
   => {:a {:c 2}}"
  {:added "0.5"}
  [atom path]
  (if (empty? path)
    (reset! atom {})
    (swap! atom map/dissoc-in path))
  atom)

(defn set-in-atom
  "drops elements in the atom
 
   (-> (atom {:a {:b 1}})
       (set-in-atom [:a] {:c 2})
       deref)
   => {:a {:c 2}}"
  {:added "0.5"}
  [atom path v]
  (if (empty? path)
    (reset! atom v)
    (swap! atom assoc-in path v))
  atom)

(defn select-in-atom
  "selects the necessary data in the data
 
   (-> (atom {:a {:b 1 :c 2}})
       (select-in-atom [:a] even?))
   => [[:c 2]]"
  {:added "0.5"}
  [atom path q]
  (let [data (get-in @atom path)]
    (reduce-kv (fn [out k v]
                 (if (match v q)
                   (conj out [k v])
                   out))
               []
               data)))

(defn batch-in-atom
  "perform batch add and remove operations
   (-> (atom {:a {:b 1 :c 2}})
       (batch-in-atom [:a]
                      {:d 3 :e 4}
                      [[:b] [:c]]))
   => {:a {:d 3, :e 4}}"
  {:added "0.5"}
  [atom path add-map del-vec]
  (let [addm (if (empty? path)
               add-map
               (assoc-in {} path add-map))]
    (swap! atom
           (fn [val]
             (reduce (fn [out v]
                       (map/dissoc-in out (concat path v)))
                     (nested/merge-nested val addm)
                     del-vec)))))

(extend-protocol keystore/IKeystore
  
  clojure.lang.Atom
  (-put-in    [store path v] (put-in-atom store path v))
  (-peek-in   [store path]    (peek-in-atom store path))
  (-keys-in   [store path]    (keys-in-atom store path))
  (-drop-in   [store path]    (drop-in-atom store path))
  (-set-in    [store path v]  (set-in-atom store path v))
  (-select-in [store path v]  (select-in-atom store path v))
  (-batch-in  [store path add-map remove-vec]
    (batch-in-atom store path add-map remove-vec)))

(defrecord MockKeystore [state]

  Object
  (toString [store]
    (str "#keystore.mock" (if state @state "<uninitiased>")))
  
  keystore/IKeystore
  (-put-in [store path v]
    (keystore/-put-in state path v)
    store)
  (-peek-in [store path]
    (keystore/-peek-in state path))
  (-keys-in  [store path]
    (keystore/-keys-in state path))
  (-drop-in [store path]
    (keystore/-drop-in state path)
    store)
  (-set-in [store path v]
    (keystore/-set-in state path v)
    store)
  (-select-in [store path q]
    (keystore/-select-in state path q))
  (-batch-in [store path add-map remove-vec]
    (keystore/-batch-in state path add-map remove-vec)
    store)

  component/IComponent
  (-start [store]
    (atom/attach-state store))

  (-stop [store]
    (atom/detach-state store)))

(defmethod print-method MockKeystore
  [v w]
  (.write w (str v)))

(defmethod keystore/create :mock
  [{:keys [file] :as m}]
  (map->MockKeystore m))
