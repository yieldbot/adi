(ns adi.utils
  (:require [clojure.string :as st]))

(defn boolean? [x] (instance? java.lang.Boolean x))

(defn hash-map? [x] (instance? clojure.lang.IPersistentMap x))

(defn hash-set? [x] (instance? clojure.lang.PersistentHashSet x))

(defn ref? [x] (or (hash-map? x) (instance? datomic.query.EntityMap x)))

(defn long? [x] (instance? java.lang.Long x))

(defn bigint? [x] (instance? clojure.lang.BigInt x))

(defn bigdec? [x] (instance? java.math.BigDecimal x))

(defn instant? [x] (instance? java.util.Date x))

(defn uuid? [x] (instance? java.util.UUID x))

(defn uri? [x] (instance? java.net.URI x))

(defn bytes? [x] (= (Class/forName "[B")
                    (.getClass x)))

;; Misc Methods

(defn gen-?sym []
  (symbol (str "?" (name (gensym)))))

(defn dissoc-in
  [m [k & ks]]
  (if-not ks
    (dissoc m k)
    (assoc m k (dissoc-in (m k) ks))))

(defn no-repeats
  ([coll] (no-repeats identity coll))
  ([f coll] (no-repeats f coll [] nil))
  ([f coll output last]
     (if-let [v (first coll)]
       (cond (and last (= (f last) (f v)))
             (no-repeats f (next coll) output last)
             :else (no-repeats f (next coll) (conj output v) v))
       output)))

(defn what-is-different
  ([d1 d2] (what-is-different d1 d2 []))
  ([d1 d2 trail]
     (cond
      (and (hash-map? d1) (hash-map? d2))
      (let [ks1 (set (keys d1)) ks2 (set (keys d2))]
        (if (= ks1 ks2)
          (mapv what-is-different  (map d1 ks1) (map d2 ks1) )
          (throw (Exception. (format "keys are different at %s %s %s" trail ks1 ks2 )))))

      (and (hash-set? d1) (hash-set? d2))
      (map #(what-is-different %1 %2 trail) d1 d2)

      (not= d1 d2)
      (throw (Exception. (format "different at %s, %s, %s " trail d1 d2))))))


;; Keyword Manipulation

(defn k-str
  "Returns the string representation without the colon.\n
   (k-str :hello/there) ;;=> \"hello/there\""
  [k]
  (if (nil? k) "" (#'st/replace-first-char (str k) \: "")))

(defn k-merge
  "Merges a vector of keywords into one keyword"
  [ks]
  (if (empty? ks) nil
      (->> (filter identity ks)
           (map k-str)
           (st/join "/")
           keyword)))

(defn k-unmerge [k]
  (if (nil? k) []
      (mapv keyword (st/split (k-str k) #"/"))))

(defn k-nsv [k]
  (or (butlast (k-unmerge k)) []))

(defn k-nsv? [k nsv]
  (= nsv (k-nsv k)))

(defn k-ns [k]
  (k-merge (k-nsv k)))

(defn k-ns?
  ([k] (< 0 (.indexOf (str k) "/")))
  ([k ns] (= ns (k-ns k))))

(defn k-z [k]
  (last (k-unmerge k)))

(defn k-z? [k z]
  (= z (k-z k)))

(defn list-ns [fsm]
  (let [ks (keys fsm)]
    (set (map k-ns ks))))

(defn list-ks [fsm ns]
  (let [ks (keys fsm)]
    (->> ks
         (filter #(= ns (k-ns %)))
         set)))

;; Map Manipulation

(defn flatten-keys
  "flatten-keys will take a map of maps and make it into a single map"
  ([m] (flatten-keys m [] {}))
  ([m nskv output]
     (if-let [[k v] (first m)]
       (cond (hash-map? v)
             (->> output
                  (flatten-keys (next m) nskv)
                  (flatten-keys v (conj nskv k)))
             :else
             (flatten-keys (next m)
                           nskv
                           (assoc output (k-merge (conj nskv k)) v)))
       output)))

(defn flatten-once-keys
  "flatten-once-keys will flatten only one layer"
  ([m] (flatten-once-keys m {}))
  ([m output]
     (if-let [[k v] (first m)]
       (cond (hash-map? v)
             (flatten-once-keys
               (next m)
               (merge (zipmap (map #(k-merge [k %]) (keys v))
                              (vals v))
                     output))

              :else
              (flatten-once-keys (next m) (assoc output k v)))
      output)))

(defn treeify-keys
  "treeify-keys will take a single map of compound keys and make it into a tree"
  ([m] (treeify-keys m {}))
  ([m output]
     (if-let [[k v] (first m)]
       (recur (rest m)
              (assoc-in output (k-unmerge k) v))
       output)))

(defn extend-keys [m nskv ex]
  (let [e-map (select-keys m ex)
        x-map (apply dissoc m ex)]
    (merge e-map (if (empty? nskv)
                   x-map
                   (assoc-in {} nskv x-map)))))

(defn contract-keys [m nskv ex]
  (let [tm     (treeify-keys m)
        c-map  (get-in tm nskv)
        x-map  (dissoc-in tm nskv)]
    (merge c-map (if (empty? ex)
                   x-map
                   (assoc-in {} ex x-map)))))
