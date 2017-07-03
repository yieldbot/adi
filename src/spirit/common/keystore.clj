(ns spirit.common.keystore
  (:require [spirit.protocol.ikeystore :as keystore]
            [spirit.common.atom :as atom]
            [clojure.java.io :as io]
            [hara.io.file :as fs]
            [hara.component :as component]
            [hara.data.nested :as nested]
            [hara.data.map :as map]))

(defn put-in
  "adds data to the database
 
   (-> (create {:type :mock})
       (put-in [:b] {:d 2})
       (put-in [:b] {:c 2})
       (peek-in))
   => {:b {:d 2, :c 2}}"
  {:added "0.5"}
  ([db v] (put-in db [] v))
  ([db arr v]
    (keystore/-put-in db arr v)
    db))

(defn peek-in
  "accesses all or part of the database
 
   (-> (create {:type :mock})
       (put-in {:a 1, :b {:c 2}})
       (peek-in [:b]))
   => {:c 2}"
  {:added "0.5"}
  ([db] (peek-in db []))
  ([db arr]
    (keystore/-peek-in db arr)))

(defn keys-in
  "returns all keys in the particular level
 
   (-> (create {:type :mock})
       (put-in {:a 1, :b {:c 2}})
       (keys-in)
       (sort))
   => [:a :b]"
  {:added "0.5"}
  ([db] (keys-in db []))
  ([db arr]
    (keystore/-keys-in db arr)))

(defn drop-in
  "removes keys in the database
   
   (-> (create {:type :mock})
       (put-in {:a 1, :b {:c 2}})
       (drop-in [:a])
       (peek-in))
   => {:b {:c 2}}"
  {:added "0.5"}
  ([db] (drop-in db []))
  ([db arr]
    (keystore/-drop-in db arr)
    db))

(defn set-in
  "adds data to the database. overwrites
 
   (-> (create {:type :mock})
       (set-in [:b] {:d 2})
       (set-in [:b] {:c 2})
       (peek-in))
   => {:b {:c 2}}"
  {:added "0.5"}
  ([db v] (set-in db [] v))
  ([db arr v]
     (keystore/-set-in db arr v)
     db))

(defn select-in
  "selects the key values corresponding to the query
   (-> (create {:type :mock})
       (set-in [:data] {:a 1
                        :b 2
                        :c 3
                        :d 4})
       (select-in [:data] odd?))
   => [[:a 1] [:c 3]]"
  {:added "0.5"}
  ([db v] (select-in db [] v))
  ([db arr v]
    (keystore/-select-in db arr v)))

(defn mutate-in
  "batch add and removal of data
 
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
       :input {:c 3, :y 2}}"
  {:added "0.5"}
  [db ks add-map del-vec]
  (keystore/-mutate-in db ks add-map del-vec)
  db)

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

(extend-protocol keystore/IKeystore
  
  clojure.lang.Atom
  (-put-in [atom arr v]
    (let [im (if (empty? arr) v
                 (assoc-in {} arr v))]
      (swap! atom nested/merge-nested im)))
  (-peek-in [atom arr]
    (if (empty? arr)
      (or @atom {})
      (get-in @atom arr)))
  (-keys-in  [atom arr]
    (-> atom (peek-in arr) keys))
  (-drop-in [atom arr]
    (if (empty? arr)
      (reset! atom {})
      (swap! atom map/dissoc-in arr)))
  (-set-in [atom arr v]
    (if (empty? arr)
      (reset! atom v)
      (swap! atom assoc-in arr v)))
  (-select-in [atom arr q]
    (let [data (get-in @atom arr)]
      (reduce-kv (fn [out k v]
                   (if (match v q)
                     (conj out [k v])
                     out))
                 []
                 data)))
  (-mutate-in [atom arr add-map del-vec]
    (let [addm (if (empty? arr)
                 add-map
                 (assoc-in {} arr add-map))]
      (swap! atom
             (fn [val]
               (reduce (fn [out v]
                         (map/dissoc-in out (concat arr v)))
                       (nested/merge-nested val addm)
                       del-vec))))))

(defrecord MockKeystore [state opts]

  Object
  (toString [db]
    (str "#mock.ks " @state))
  
  keystore/IKeystore
  (-put-in [db arr v]
    (put-in state arr v))
  (-peek-in [db arr]
    (peek-in state arr))
  (-keys-in  [db arr]
    (keys-in state arr))
  (-drop-in [db arr]
    (drop-in state arr))
  (-set-in [db arr v]
    (set-in state arr v))
  (-select-in [db arr q]
    (select-in state arr q))
  (-mutate-in [db arr add-map del-vec]
    (mutate-in state arr add-map del-vec)))

(defmethod print-method MockKeystore
  [v w]
  (.write w (str v)))

(defmulti create
  "creates a keystore
 
   (create {:type :atom})
 
   (create {:type :mock
            :file \"test.edn\"})"
  {:added "0.5"}
  :type)

(defmethod create :raw
  [{:keys [data]}]
  (atom (or data {})))

(defmethod create :mock
  [{:keys [file data reset] :as opts}]
  (let [_ (if reset (fs/delete file))
        state (cond-> (atom (or data {}))
                file
                (atom/file-out opts))]
    (MockKeystore. state opts)))

(defn keystore
  "creates a standalone keystore
 
   (keystore)
   => MockKeystore
 
   (keystore {:type :atom})
   => clojure.lang.Atom"
  {:added "0.5"}
  ([]
   (create {:type :mock}))
  ([m]
   (-> (create m)
       (component/start))))
