(ns spirit.data.keystore.base
  (:require [spirit.protocol.ikeystore :as keystore]
            [spirit.common.atom :as atom]
            [hara.io.file :as fs]
            [hara.data.nested :as nested]
            [hara.data.map :as map]))

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
    (-> atom (keystore/-peek-in arr) keys))
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
    (str "#keystore.mock" @state))
  
  keystore/IKeystore
  (-put-in [db arr v]
    (keystore/-put-in state arr v))
  (-peek-in [db arr]
    (keystore/-peek-in state arr))
  (-keys-in  [db arr]
    (keystore/-keys-in state arr))
  (-drop-in [db arr]
    (keystore/-drop-in state arr))
  (-set-in [db arr v]
    (keystore/-set-in state arr v))
  (-select-in [db arr q]
    (keystore/-select-in state arr q))
  (-mutate-in [db arr add-map del-vec]
    (keystore/-mutate-in state arr add-map del-vec)))

(defmethod print-method MockKeystore
  [v w]
  (.write w (str v)))

(defmethod keystore/create :raw
  [{:keys [data]}]
  (atom (or data {})))

(defmethod keystore/create :mock
  [{:keys [file data reset] :as opts}]
  (let [_ (if reset (fs/delete file))
        state (cond-> (atom (or data {}))
                file
                (atom/file-out opts))]
    (MockKeystore. state opts)))
