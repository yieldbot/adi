(ns adi.utils
  (:use [hara.common :only [hash-map? long?]]
        [hara.hash-map :only [keyword-contains? keyword-ns]]
        [datomic.api :only [tempid]]))

(defn enum?
  "Returns `true` if `x` is an enum keyword"
  [x] (or (keyword? x) (long? x)))

(defn db-id?
  "Returns `true` if `x` implements `datomic.db.DbId"
  [x]  (instance? datomic.db.DbId x))

(defn entity?
  "Returns `true` if `x` is of type `datomic.query.EntityMap`."
  [x] (instance? datomic.query.EntityMap x))

(defn ref?
  "Returns `true` if `x` implements `clojure.lang.APersistentMap`
   or is of type `datomic.query.EntityMap` or is a long or db-id."
  [x] (or (hash-map? x) (entity? x) (db-id? x) (long? x)))

(defn iid
  "Constructs a new id"
  ([] (tempid :db.part/user))
  ([obj]
     (let [v (if (number? obj) (long obj) (hash obj))
           v (if (< 0 v) (- v) v)]
       (tempid :db.part/user v ))))

(defn ?q-fn [f args]
 [(apply list 'list
         (list 'symbol (str f))
         (list 'symbol "?") args)])

(defmacro ?q [f & args]
 [[(list 'symbol "??sym")
   (list 'symbol "??attr")
   (list 'symbol "?")]
  (?q-fn f args)])

(defn ?not [val]
 (?q not= val))

(defn ?fulltext [val]
 [[(list 'fulltext '$ '??attr val) [['??sym '?]]]])

(defn ?gensym
  "Returns a new datomic symbol with a unique name. If a prefix string
   is supplied, the name is `prefix#` where `#` is some unique number. If
   prefix is not supplied, the prefix is `e_`.

    (?gensym) ;=> ?e_1238

    (?gensym) ;=> ?e_1241

    (?gensym \"v\") ;=> ?v1250
   "
  ([] (?gensym 'e_))
  ([prefix] (symbol (str "?" (name (gensym prefix))))))

(defn incremental-sym-gen
  ([s] (incremental-sym-gen s 0))
  ([s n]
     (let [r (atom n)]
       (fn []
         (swap! r inc)
         (symbol (str "?" s @r))))))

(defn incremental-id-gen
  ([] (incremental-id-gen 0))
  ([n]
     (let [r (atom n)]
       (fn []
         (swap! r inc)
         @r))))

(defn expand-ns-keys
  ([k] (expand-ns-keys k #{}))
  ([k output]
     (if (nil? k) output
       (if-let [nsk (keyword-ns k)]
         (expand-ns-keys nsk (conj output k))
         (conj output k)))))

(defn expand-ns-set
  ([s] (expand-ns-set s #{}))
  ([s output]
     (if-let [k (first s)]
       (expand-ns-set (next s)
                      (clojure.set/union output
                                         (expand-ns-keys k)))
       output)))

(defn merge-common-ns-keys
  ([m k kset] (merge-common-ns-keys m k kset {}))
  ([m k kset output]
     (if-let [[j jset] (first m)]
       (let [njset  (if (keyword-contains? j k)
                       (clojure.set/union kset jset)
                       jset)]
         (merge-common-ns-keys (next m) k kset (assoc output j njset)))
       output)))

(defn merge-common-nss
  ([m] (merge-common-nss m m))
  ([m output]
     (if-let [[k kset] (first m)]
       (merge-common-nss (next m)
                         (merge-common-ns-keys output k kset))
       output)))

(defn flatten-to-vecs
 "Takes a sequence of vectors. If any vector contains additional
  vectors, then that vector is flattened.

   (flatten-to-vecs [[1 2 3] [[1 2 3] [4 5 6]]])
   ;=> [[1 2 3] [1 2 3] [4 5 6]]
 "
 ([coll] (flatten-to-vecs coll []) )
 ([[v & vs] output]
    (cond (nil? v) output
          (vector? (first v)) (flatten-to-vecs vs (concat output v))
          :else (flatten-to-vecs vs (conj output v)))))

(defn walk-replace [st rep]
  (cond (vector? st) (mapv #(walk-replace % rep) st)
        (list? st) (map #(walk-replace % rep) st)
        (hash-map? st) (zipmap (keys st)
                               (map #(walk-replace % rep) (vals st)))
        (rep st) (rep st)
        :else st))

(defn auto-pair-seq
  ([arr] (auto-pair-seq [] arr))
  ([output [x y & xs :as arr]]
     (cond (nil? x) output
           (or (keyword? y)
               (and (nil? y) (nil? xs)))
           (recur (conj output [x true]) (next arr))
           :else (recur (conj output [x y]) xs))))
