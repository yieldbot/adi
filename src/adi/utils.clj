(ns adi.utils
  (:require [clojure.string :as st]))

;; ## Type Predicates

(defn boolean?
  "Returns `true` if `x` is of type `java.lang.Boolean`."
  [x] (instance? java.lang.Boolean x))

(defn hash-map?
  "Returns `true` if `x` implements `clojure.lang.IPersistentMap`."
  [x]
  (and (instance? clojure.lang.IPersistentMap x)
       (not (instance? datomic.db.DbId x))))

(defn hash-set?
  "Returns `true` if `x` implements `clojure.lang.IPersistentHashSet`."
  [x] (instance? clojure.lang.PersistentHashSet x))

(defn entity?
  "Returns `true` if `x` is of type `datomic.query.EntityMap`."
  [x] (instance? datomic.query.EntityMap x))

(defn ref?
  "Returns `true` if `x` implements `clojure.lang.IPersistentMap`
   or is of type `datomic.query.EntityMap`."
  [x] (or (hash-map? x) (entity? x)))

(defn long?
  "Returns `true` if `x` is of type `java.lang.Long`."
  [x] (instance? java.lang.Long x))

(defn bigint?
  "Returns `true` if `x` is of type `clojure.lang.BigInt`."
  [x] (instance? clojure.lang.BigInt x))

(defn bigdec?
  "Returns `true` if `x` is of type `java.math.BigDecimal`."
  [x] (instance? java.math.BigDecimal x))

(defn instant?
  "Returns `true` if `x` is of type `java.util.Date`."
  [x] (instance? java.util.Date x))

(defn uuid?
  "Returns `true` if `x` is of type `java.util.UUID`."
  [x] (instance? java.util.UUID x))

(defn uri?
  "Returns `true` if `x` is of type `java.net.URI`."
  [x] (instance? java.net.URI x))

(defn bytes?
  "Returns `true` if `x` is a primitive `byte` array."
  [x] (= (Class/forName "[B")
         (.getClass x)))

(defn enum?
  "Returns `true` if `x` is an enum keyword"
  [x] (keyword? x))

(defn lazy-seq?
  "Returns `true` if `x` is of type `clojure.lang.LazySeq`."
  [x] (instance? clojure.lang.LazySeq x))

(defn type-checker
  "Returns the checking function associated with keyword `k`

    (type-checker :string)
    ;=> #'clojure.core/string?

    (type-checker :bytes)
    ;=> #'adi.utils/bytes?
  "
  [k]
  (resolve (symbol (str (name k) "?"))))

;; ## Misc Methods

(defn error
  "Throws an error when called. Syntactic sugar "
  ([e] (throw (Exception. (str e))))
  ([e & more]
     (throw (Exception. (apply str e more)))))

(defn funcmap
  "Returns a hash-map `m`, with the the values of `m` being
   the items within the collection and keys of `m` constructed
   by mapping `f` to `coll`. This is used to turn a collection
   into a lookup for better search performance.

    (funcmap :id [{:id 1 :val 1} {:id 2 :val 2}])
    ;=> {1 {:id 1 :val 1} 2 {:id 2 :val 2}}
   "
  [f coll] (zipmap (map f coll) coll))

(defn replace-all
  "Returns a string with all instances of `old` in `s` replaced with
   the value of `new`."
  [s old new]
  (.replaceAll s old new))

(defn starts-with
  "Returns `true` if `s` begins with `pre`."
  [s pre]
  (.startsWith s pre))

(defn slugify-name
  "Returns a string consisiting of hyphens and lower-case letters."
  [s]
  (clojure.string/join
   "-" (-> (replace-all s "&" " and ")
           (replace-all "'" "")
           clojure.string/lower-case
           (clojure.string/split #"\s+"))))

(defn ?sym
  "Returns a new datomic symbol with a unique name. If a prefix string
   is supplied, the name is `prefix#` where `#` is some unique number. If
   prefix is not supplied, the prefix is `e_`.

    (?sym) ;=> ?e_1238

    (?sym) ;=> ?e_1241

    (?sym \"v\") ;=> ?v1250
   "
  ([] (?sym 'e_))
  ([prefix] (symbol (str "?" (name (gensym prefix))))))

(defn dissoc-in
  "Dissociates a key in a nested associative structure `m`, where `[k & ks]` is a
  sequence of keys. If any levels are empty after the operation, they will be removed.

    (dissoc-in {:a {:b {:c 3}}} [:a :b :c])
    ;=> {}

  "
  [m [k & ks]]
  (if-not ks
    (dissoc m k)
    (let [nm (dissoc-in (m k) ks)]
      (cond (empty? nm) (dissoc m k)
            :else (assoc m k nm)))))

(defn dissoc-in-keep
  "Dissociates a key in a nested associative structure `m`, where `[k & ks]` is a
  sequence of keys. Empty levels will not be removed.

    (dissoc-in-keep {:a {:b {:c 3}}}
                    [:a :b :c])
    ;=> {:a {:b {}}})
  "
  [m [k & ks]]
  (if-not ks
    (dissoc m k)
    (assoc m k (dissoc-in-keep (m k) ks))))

(defn remove-repeated
  "Returns a vector of the items in `coll` for which `(f item)` is unique
   for sequential `item`'s in `coll`.

    (remove-repeated [1 1 2 2 3 3 4 5 6])
    ;=> [1 2 3 4 5 6]

    (remove-repeated even? [2 4 6 1 3 5])
    ;=> [2 1]
   "
  ([coll] (remove-repeated identity coll))
  ([f coll] (remove-repeated f coll [] nil))
  ([f coll output last]
     (if-let [v (first coll)]
       (cond (and last (= (f last) (f v)))
             (remove-repeated f (next coll) output last)
             :else (remove-repeated f (next coll) (conj output v) v))
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

;; ## Keyword Methods

(defn keyword-str
  "Returns the string representation of the keyword
   without the colon.

    (keyword-str :hello/there)
    ;=> \"hello/there\"
  "
  [k]
  (if (nil? k) "" (#'st/replace-first-char (str k) \: "")))

(defn keyword-join
  "Merges a sequence of keywords into one.

    (keyword-join [:hello :there])
    ;=> :hello/there

    (keyword-join [:a :b :c :d])
    ;=> :a/b/c/d)"
  ([ks] (keyword-join ks "/"))
  ([ks sep]
     (if (empty? ks) nil
         (->> (filter identity ks)
              (map keyword-str)
              (st/join sep)
              keyword))))

(defn keyword-split
  "The opposite of `keyword-join`. Splits a keyword
   by the `/` character into a vector of keys.

    (keyword-split :hello/there)
    ;=> [:hello :there]

    (keyword-split :a/b/c/d)
    ;=> [:a :b :c :d]
  "
  ([k] (keyword-split k #"/"))
  ([k re]
     (if (nil? k) []
         (mapv keyword (st/split (keyword-str k) re)))))

(defn keyword-nsvec
  "Returns the namespace vector of keyword `k`.

    (keyword-nsvec :hello/there)
    ;=> [:hello]

    (keyword-nsvec :hello/there/again)
    ;=> [:hello :there]
  "
  [k]
  (or (butlast (keyword-split k)) []))

(defn keyword-nsvec?
  "Returns `true` if keyword `k` has the namespace vector `nsv`."
  [k nsv]
  (= nsv (keyword-nsvec k)))

(defn keyword-nsroot
  "Returns the namespace root of `k`.

    (keyword-nsroot :hello/there)
    ;=> :hello

    (keyword-nsroot :hello/there/again)
    ;=> :hello
  "
  [k]
  (first (keyword-nsvec k)))

(defn keyword-nsroot?
  "Returns `true` if keyword `k` has the namespace base `nsk`."
  [k nsk]
  (= nsk (keyword-nsroot k)))

(defn keyword-stemvec
  "Returns the stem vector of `k`.

    (keyword-stemvec :hello/there)
    ;=> [:there]

    (keyword-stemvec :hello/there/again)
    ;=> [:there :again]
  "
  [k]
  (rest (keyword-split k)))

(defn keyword-stemvec?
  "Returns `true` if keyword `k` has the stem vector `kv`."
  [k kv]
  (= kv (keyword-stemvec k)))

(defn keyword-stem
  "Returns the steam of `k`.

    (keyword-stem :hello/there)
    ;=> :there

    (keyword-stem :hello/there/again)
    ;=> :there/again
  "
  [k]
  (keyword-join (keyword-stemvec k)))

(defn keyword-stem?
  "Returns `true` if keyword `k` has the stem `kst`."
  [k kst]
  (= kst (keyword-stem k)))

(defn keyword-ns
  "Returns the namespace of `k`.

    (keyword-ns :hello/there)
    ;=> :hello

    (keyword-ns :hello/there/again)
    ;=> :hello/there
  "
  [k]
  (keyword-join (keyword-nsvec k)))

(defn keyword-ns?
  "Returns `true` if keyword `k` has a namespace or
   if `ns` is given, returns `true` if the namespace
   of `k` is equal to `ns`.

    (keyword-ns? :hello)
    ;=> false

    (keyword-ns? :hello/there)
    ;=> true

    (keyword-ns? :hello/there :hello)
    ;=> true
  "
  ([k] (< 0 (.indexOf (str k) "/")))
  ([k ns] (if-let [tkns (keyword-ns k)]
            (= 0 (.indexOf (str k)
                 (str ns "/")))
            (nil? ns))))

(defn keyword-val
  "Returns the keyword value of the `k`.

    (keyword-val :hello)
    ;=> :hello

    (keyword-val :hello/there)
    ;=> :there"
   [k]
  (last (keyword-split k)))

(defn keyword-val?
  "Returns `true` if the keyword value of `k` is equal
   to `z`."
  [k z]
  (= z (keyword-val k)))

(defn list-keyword-ns
  "Returns the set of keyword namespaces within `fm`.

    (list-keyword-ns {:hello/a 1 :hello/b 2
                      :there/a 3 :there/b 4})
    ;=> #{:hello :there}
  "
  [fm]
  (let [ks (keys fm)]
    (set (map keyword-ns ks))))

(defn list-ns-keys
  "Returns the set of keys in `fm` that has keyword namespace
  of `ns`.

    (list-ns-keys {:hello/a 1 :hello/b 2
                   :there/a 3 :there/b 4} :hello)
    ;=> #{:hello/a :hello/b})
  "
  [fm ns]
  (let [ks (keys fm)]
    (->> ks
         (filter #(= ns (keyword-ns %)))
         set)))


(defn contain-ns-keys?
  "Returns `true` if any key in `fm` has keyword namespace
  of `ns`.

    (contain-ns-keys? {:hello/a 1 :hello/b 2
                       :there/a 3 :there/b 4} :hello)
    ;=> true
  "
  [fm ns]
  (some #(keyword-ns? % ns) (keys fm)))


(defn list-keys-in
  "Return the set of all nested keys in `m`.

    (list-keys-in {:a {:b 1 :c {:d 1}}})
    ;=> #{:a :b :c :d})"

  ([m] (list-keys-in m #{}))
  ([m ks]
     (if-let [[k v] (first m)]
       (cond (hash-map? v)
             (clojure.set/union
              (list-keys-in (next m) (conj ks k))
              (list-keys-in v))

             :else (list-keys-in (next m) (conj ks k)))
       ks)))

(defn dissoc-keys-in
   "Returns `m` without all nested keys in `ks`.

    (dissoc-keys-in {:a {:b 1 :c {:b 1}}} [:b])
    ;=> {:a {:c {}}}"

  ([m ks] (dissoc-keys-in m (set ks) {}))
  ([m ks output]
    (if-let [[k v] (first m)]
      (cond (ks k) (dissoc-keys-in (next m) ks output)

            (hash-map? v)
            (dissoc-keys-in (next m) ks
              (assoc output k (dissoc-keys-in v ks)))
            :else
            (dissoc-keys-in (next m) ks (assoc output k v)))
      output)))

;; ## Map Manipulation

(defn flatten-keys
  "Returns `m` with the first nest layer of keys flattened
   onto the root layer.

    (flatten-keys {:a {:b 2 :c 3} e: 4})
    ;=> {:a/b 2 :a/c 3 :e 4}

    (flatten-keys {:a {:b {:c 3 :d 4}
                           :e {:f 5 :g 6}}
                   :h {:i 7}
                   :j 8})
    ;=> {:a/b {:c 3 :d 4} :a/e {:f 5 :g 6} :h/i 7 :j 8})
  "
  ([m] (flatten-keys m {}))
  ([m output]
     (if-let [[k v] (first m)]
       (cond (hash-map? v)
             (flatten-keys
               (next m)
               (merge (zipmap (map #(keyword-join [k %]) (keys v))
                              (vals v))
                       output))

              :else
              (flatten-keys (next m) (assoc output k v)))
      output)))

(defn flatten-keys-in
  "Returns a single associative map with all of the nested
  keys of `m` flattened.

    (flatten-keys-in {:a {:b {:c 3 :d 4}
                              :e {:f 5 :g 6}}
                      :h {:i 7}
                      :j 8 })
    ;=> {:a/b/c 3 :a/b/d 4 :a/e/f 5 :a/e/g 6 :h/i 7 :j 8}
  "
  ([m] (flatten-keys-in m [] {}))
  ([m nskv output]
     (if-let [[k v] (first m)]
       (cond (hash-map? v)
             (->> output
                  (flatten-keys-in (next m) nskv)
                  (flatten-keys-in v (conj nskv k)))

             (nil? v)
             (flatten-keys-in (next m) nskv output)

             :else
             (flatten-keys-in (next m)
                           nskv
                           (assoc output (keyword-join (conj nskv k)) v)))
       output)))

(defn treeify-keys
  "Returns a nested map, expanding out the first
   level of keys into additional hash-maps.

    (treeify-keys {:a/b 2 :a/c 3})
    ;=> {:a {:b 2 :c 3}}

    (treeify-keys {:a/b {:e/f 1} :a/c {:g/h 1}})
    ;=> {:a {:b {:e/f 1}
             :c {:g/h 1}}}

  "
  ([m] (treeify-keys m {}))
  ([m output]
     (if-let [[k v] (first m)]
       (recur (rest m)
              (assoc-in output (keyword-split k) v))
       output)))

(defn treeify-keys-in
  "Returns a nested map, expanding out all
   levels of keys into additional hash-maps.

    (treeify-keys-in {:a/b 2 :a/c 3})
    ;=> {:a {:b 2 :c 3}}

    (treeify-keys-in {:a/b {:e/f 1} :a/c {:g/h 1}})
    ;=> {:a {:b {:e {:f 1}}
             :c {:g {:h 1}}}}

  "
  [m]
  (let [kvs (seq m)
        hm? #(hash-map? (second %))
        ms  (filter hm? kvs)
        vs  (filter (complement hm?) kvs)
        outm (reduce #(assoc-in %1 (keyword-split (first %2))
                                  (treeify-keys-in (second %2)))
                    {} ms)]
    (reduce #(assoc-in %1 (keyword-split (first %2)) (second %2))
            outm vs)))

(defn diff-in
  "Returns any nested values in `m1` that are different to those in `m2`.

    (diff-in {:a {:b 1}}
             {:a {:b 1 :c 1}})
    ;=> {}

    (diff-in {:a {:b 1 :c 1}}
             {:a {:b 1}})
    ;=> {:a {:c 1}}"
  ([m1 m2] (diff-in m1 m2 {}))
  ([m1 m2 output]
     (if-let [[k v] (first m1)]
       (cond (nil? (k m2))
             (diff-in (dissoc m1 k) m2 (assoc output k v))

             (and (hash-map? v) (hash-map? (k m2)))
             (let [sub (diff-in v (k m2))]
               (if (empty? sub)
                 (diff-in (dissoc m1 k) m2 output)
                 (diff-in (dissoc m1 k) m2 (assoc output k sub))))

             (not= v (k m2))
             (diff-in (dissoc m1 k) m2 (assoc output k v))

             :else
             (diff-in (dissoc m1 k) m2 output))
       output)))

(defn merge-in
  "Merges all nested values in `m1`, `m2` and `ms` if there are more.
   nested values of maps on the right will replace those on the left if
   the keys are the same.

    (merge-in {:a {:b {:c 3}}} {:a {:b 3}})
    ;=> {:a {:b 3}}

    (merge-in {:a {:b {:c 1 :d 2}}}
              {:a {:b {:c 3}}})
    ;=> {:a {:b {:c 3 :d 2}}})
  "
  ([m1 m2]
  (if-let [[k v] (first m2)]
    (cond (nil? (k m1))
          (merge-in (assoc m1 k v) (dissoc m2 k))

          (and (hash-map? v) (hash-map? (k m1)))
          (merge-in (assoc m1 k (merge-in (k m1) v)) (dissoc m2 k))

          (not= v (k m1))
          (merge-in (assoc m1 k v) (dissoc m2 k))

          :else
          (merge-in m1 (dissoc m2 k)))
    m1))
  ([m1 m2 & ms]
   (cond (empty? ms) (merge-in m1 m2)
          :else (apply (merge-in (merge-in m1 m2)) ms))))


(defn remove-empty-in
  "Returns a associative with nils and empty hash-maps removed.

    (remove-empty-in {:a {:b {:c {}}}})
    ;=> {}

    (remove-empty-in {:a {:b {:c {} :d 1}}})
    ;=> {:a {:b {:d 1}}}
  "
  ([m] (remove-empty-in m {}))
  ([m output]
     (if-let [[k v] (first m)]
       (cond (nil? v)
             (remove-empty-in (dissoc m k) output)

             (hash-map? v)
             (let [rmm (remove-empty-in v)]
               (if (empty? rmm)
                 (remove-empty-in (dissoc m k) output)
                 (remove-empty-in (dissoc m k) (assoc output k rmm))))

             :else
             (remove-empty-in (dissoc m k) (assoc output k v)))
       output)))


(defn nest-keys-in
  "Returns a map that takes `m` and extends all keys with the
   `nskv` vector. `ex` is the list of keys that are not extended.

    (nest-keys-in {:a 1 :b 2} [:hello :there])
    ;=> {:hello {:there {:a 1 :b 2}}}

    (nest-keys-in {:there 1 :b 2} [:hello] [:there])
    ;=> {:hello {:b 2} :there 1}
  "
  ([m nskv] (nest-keys-in m nskv []))
  ([m nskv ex]
    (let [e-map (select-keys m ex)
          x-map (apply dissoc m ex)]
      (merge e-map (if (empty? nskv)
                     x-map
                     (assoc-in {} nskv x-map))))))

(defn unnest-keys-in
  "The reverse of `nest-keys-in`. Takes `m` and returns a map
   with all keys with a `keyword-nsvec` of `nskv` being 'unnested'

    (unnest-keys-in {:hello/a 1
                     :hello/b 2
                     :there/a 3
                     :there/b 4} [:hello])
    ;=> {:a 1 :b 2
         :there {:c 3 :d 4}}

    (unnest-keys-in {:hello {:there {:a 1 :b 2}}
                     :again {:c 3 :d 4}} [:hello :there] [:+] )
    ;=> {:a 1 :b 2
         :+ {:again {:c 3 :d 4}}}
  "
  ([m nskv] (unnest-keys-in m nskv []))
  ([m nskv ex]
   (let [tm     (treeify-keys-in m)
         c-map  (get-in tm nskv)
         x-map  (dissoc-in tm nskv)]
    (merge c-map (if (empty? ex)
                   x-map
                   (assoc-in {} ex x-map))))))
