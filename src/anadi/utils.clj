(ns anadi.utils
  (:require [clojure.string :as st]))

(def leaf? vector?)

(defn boolean? [x] (instance? java.lang.Boolean x))

(defn hash-map? [x] (instance? clojure.lang.IPersistentMap x))

(defn ref? [x] (hash-map? x))

(defn long? [x] (instance? java.lang.Long x))

(defn bigint? [x] (instance? java.math.BigInteger x))

(defn bigdec? [x] (instance? java.math.BigDecimal x))

(defn instant? [x] (instance? java.util.Date x))

(defn uuid? [x] (instance? java.util.UUID x))

(defn uri? [x] (instance? java.net.URI x))

(defn bytes? [x] (= (Class/forName "[B")
                    (.getClass x)))

(defn key-str
  "Returns the string representation without the colon.\n
   (key-str :hello/there) ;;=> \"hello/there\""
  [k]
  (if (nil? k) "" (subs (str k) 1)))

(defn merge-keys
  "Merges a vector of keywords into one keyword"
  [ks]
  (if (empty? ks) nil
      (->> (filter identity ks)
           (map key-str)
           (st/join "/")
           keyword)))

(defn seperate-keys [k]
  (if (nil? k) []
      (mapv keyword (st/split (key-str k) #"/"))))

(defn key-ns? [k]
  (< 0 (.indexOf (str k) "/")))

(defn key-ns [k]
  (merge-keys (butlast (seperate-keys k))))

(defn key-name [k]
  (last (seperate-keys k)))

(defn flatten-keys
  "flatten-keys will take a map of maps and make it into a single map"
  ([m] (flatten-keys m [] {}))
  ([m kns output]
     (if-let [[k v] (first m)]
       (cond (hash-map? v)
             (->> output
                  (flatten-keys (next m) kns)
                  (flatten-keys v (conj kns k)))
             :else
             (flatten-keys (next m)
                           kns
                           (assoc output (merge-keys (conj kns k)) v)))
       output)))

(defn treeify-keys
  "treeify-keys will take a single map of compound keys and make it into a tree"
  ([m] (treeify-keys m {}))
  ([m output]
     (if-let [[k v] (first m)]
       (recur (rest m)
              (assoc-in output (seperate-keys k) v))
       output)))

(defn extend-key-ns [m kns ex]
  (let [e-map (select-keys m ex)
        x-map (apply dissoc m ex)]
    (merge e-map (if (empty? kns)
                   x-map
                   (assoc-in {} kns x-map)))))

(defn contract-key-ns [m kns ex]
  (let []))
