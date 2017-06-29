(ns spirit.common.keystore
  (:require [spirit.protocol.ikeystore :as keystore]
            [spirit.common.atom :as atom]
            [clojure.java.io :as io]
            [hara.data.nested :as nested]
            [hara.data.map :as map]))

(defn put-in
  ([db v] (put-in db [] v))
  ([db arr v]
    (keystore/-put-in db arr v)
    db))

(defn peek-in
  ([db] (peek-in db []))
  ([db arr]
    (keystore/-peek-in db arr)))

(defn keys-in
  ([db] (keys-in db []))
  ([db arr]
    (keystore/-keys-in db arr)))

(defn drop-in
  ([db] (drop-in db []))
  ([db arr]
    (keystore/-drop-in db arr)
    db))

(defn set-in
  ([db v] (set-in db [] v))
  ([db arr v]
     (keystore/-set-in db arr v)
     db))

(defn select-in
  ([db v] (select-in db [] v))
  ([db arr v]
    (keystore/-select-in db arr v)))

(defn mutate-in
  [db ks add-map del-vec]
  (keystore/-mutate-in db ks add-map del-vec)
  db)

(defn match [data query]
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
                     (conj out v)
                     out))
                 []
                 data)))
  (-mutate-in [atom arr add-map del-vec]
    (let [addm {arr add-map}]
      (swap! atom
             (fn [val]
               (reduce (fn [out v]
                         (map/dissoc-in out (cons arr v)))
                       (nested/merge-nested val addm)
                       del-vec))))))

(defrecord MockKeystore [state opts]

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
    (mutate-in state arr)))

(defmulti keystore :type)

(defmethod keystore :atom
  [{:keys [data]}]
  (atom (or data {})))

(defmethod keystore :mock
  [{:keys [file initial reset] :as opts}]
  (let [state (if file
                (atom/file-backed (atom nil) (assoc opts :file file))
                (atom {}))]
    (MockKeystore. state opts)))

(comment

  (def a (create {:type :mock
                  ;;:file "var.db"
                  :initial {:a 1 :b 2 :c 3}}))
  
  
  
  (put-in a [:b 2])

  
  (def a (MockKeystore. (atom {})))


  (-> (put-in a [:a :b] {:hello :there})
      (select-in [:a :b] {:hello "there"})
      (peek-in [:a])))
