(ns adi.data
  (:use [datomic.api :only [tempid]]
        [hara.control :only [if-let]]
        adi.utils)
  (:require [adi.schema :as as])
  (:refer-clojure :exclude [if-let]))

(defn iid
  "Constructs a new id"
  ([] (tempid :db.part/user))
  ([obj]
     (let [v (if (number? obj) (long obj) (hash obj))
           v (if (< 0 v) (- v) v)]
       (tempid :db.part/user v ))))

;; ## Adjust Functions

(declare adjust
         adjust-chk-type adjust-chk-restrict
         adjust-value adjust-value-sets-only adjust-value-normal)

(defn adjust
  "Adjusts the `v` according to `:cardinality` in `meta` or the `:sets-only`
   flag in `(env :options)`. Checks to see if the value is of correct type
   and has an optional `:restrict` parameter as well as ; `:restrict?` flag,
   also defined in `(env :options)`."
  [v meta env]
  (-> (adjust-chk-type v meta env)
      (adjust-chk-restrict meta env)))

(defn adjust-chk-type [v meta env]
  (let [chk      (as/geni-type-checks (:type meta))
        err-one  (format "The value %s is not of type %s, it is of type %s"
                        v (:type meta) (type v))
        err-many (format "The value/s [%s] are not of type %s"
                         v meta)]
    (adjust-value v meta chk env err-one err-many)))

(defn adjust-chk-restrict [v meta env]
  (if-let [restrict? (-> env :options :restrict?)
           chk (or (:restrict meta) (-> meta :enum :values))]
    (let [err-one  (format "The value %s does not meet the restriction %s" v chk)
          err-many (format "The value/s [%s] do not meet the restriction %s" v chk)]
      (adjust-value v meta chk env err-one err-many))
    v))

(defn adjust-value [v meta chk env err-one err-many]
  (if (-> env :options :sets-only?)
      (adjust-value-sets-only v chk err-many)
      (adjust-value-normal v meta chk err-one err-many)))

(defn adjust-safe-check [chk v]
  (or (try (chk v) (catch Exception e)) (= v '_)))

(defn adjust-value-sets-only [v chk err-many]
  (cond (adjust-safe-check chk v) #{v}
        (and (set? v) (every? #(adjust-safe-check chk %) v)) v
        :else (throw (Exception. err-many))))

(defn adjust-value-normal [v meta chk err-one err-many]
  (let [c (or (:cardinality meta) :one)]
    (cond (= c :one)
          (if (adjust-safe-check chk v) v
              (throw (Exception. err-one)))

          (= c :many)
          (cond (adjust-safe-check chk v) #{v}
                (and (set? v) (every? #(adjust-safe-check chk %) v)) v
                :else (throw (Exception. err-many))))))

(declare process
         process-init process-init-assoc process-init-ref
         process-extras process-extras-current)

(defn process-merge-required
  [pdata fgeni ks env]
  (if (empty? ks) pdata
      (error "The following keys are required: " ks)))

(defn process-merge-defaults
  [pdata fgeni ks env]
  (if-let [k (first ks)]
    (let [[meta] (fgeni k)
          t      (:type meta)
          dft    (:default meta)
          value  (if (fn? dft) (dft) dft)
          value  (if (and (not (set? value))
                          (or (-> env :options :sets-only?)
                              (= :many (:cardinality meta))))
                   #{value} value)
          npdata  (cond (or (= t :keyword) (= t :enum))
                       (process-assoc-keyword pdata meta k value)
                       :else
                       (assoc pdata k value))]
      (process-merge-defaults npdata fgeni (next ks) env))
    pdata))

(defn process
  ([data] (process data {}))
  ([data env]
     (let [geni  (-> env :schema :geni)
           fgeni (-> env :schema :fgeni)
           ndata  (process-init data geni env)
           ndata  (if (-> env :options :defaults?)
                    (process-extras ndata fgeni
                                    {:label :default
                                     :function process-merge-defaults}
                                    env)
                    ndata)
           ndata  (if (-> env :options :required?)
                    (process-extras ndata fgeni
                                    {:label :required
                                     :function process-merge-required}
                                    env)
                    ndata)]
       ndata)))

(defn process-unnest-key
  ([data] (process-unnest-key data :+))
  ([data k]
     (if-let [ndata (data k)]
       (merge (dissoc data k)
              (process-unnest-key ndata k))
       data)))

(defn process-make-key-tree
  ([data geni]
     (let [tdata (-> (treeify-keys-in data)
                     (process-unnest-key :+))]
       (process-make-key-tree tdata geni {})))
  ([data geni output]
     (if-let [[k v] (first data)]
       (cond (nil? (k geni))
             (process-make-key-tree (next data) geni output)

             (vector? (k geni))
             (process-make-key-tree (next data) geni (assoc output k true))

             (hash-map? (k geni))
             (let [rv (process-make-key-tree v (k geni) {})
                   noutput (assoc output k rv)]
               (process-make-key-tree (next data) geni noutput)))
       output)))

(defn process-find-nss
  ([m] (process-find-nss m #{}))
  ([m output]
     (if-let [[k v] (first m)]
       (cond (hash-map? v)
             (process-find-nss (next m) (conj output k))
             :else
             (if-let [ns (keyword-ns k)]
               (process-find-nss (next m) (conj output ns))
               (process-find-nss (next m) output)))
       output)))

(defn process-make-nss [data geni]
  (-> (process-make-key-tree data geni)
      (flatten-keys-in-keep)
      (process-find-nss)))

(defn process-assoc-keyword
  ([output meta k v]
     (let [t   (:type meta)
           kns (-> meta t :ns)]
       (cond (set? v)
             (assoc output k (set (map #(keyword-join [kns %]) v)))

             :else
             (assoc output k (keyword-join [kns v]))))))

(defn process-init-env [geni env]
  (let [schema  (or (:schema env) (as/make-scheme-model geni))
        opts    (or (:options env) {})
        mopts   {:defaults? (if (nil? (:defaults? opts)) true (:defaults? opts))
                 :restrict? (if (nil? (:restrict? opts)) true (:restrict? opts))
                 :required? (if (nil? (:required? opts)) true (:required? opts))
                 :extras? (or (:extras? opts) false)
                 :sets-only? (or (:sets-only? opts) false)}]
    (assoc env :schema schema :options mopts)))

(defn process-init
  ([data geni env]
     (let [nss (process-make-nss data geni)]
       (-> (process-init {} (treeify-keys-in data) geni env)
           (assoc-in [:# :nss] nss))))
  ([output data geni env]
     (if-let [[k v] (first data)]
       (cond (= k :+)
             (merge (process-init {} v (-> env :schema :geni) env)
                    (process-init output (next data) geni env))

             (or (= k :#) (= k :db)) ;; add and continue
             (assoc (process-init output (next data) geni env) k v)

             (vector? (geni k))
             (-> (process-init-assoc output (geni k) v env)
                 (process-init (next data) geni env))

             (hash-map? (geni k))
             (merge (process-init {} v (geni k) env)
                    (process-init output (next data) geni env))

             (not (contains? geni k))
             (if (-> env :options :extras?)
               (process-init output (next data) geni env)
               (error "(" k ", " v ") not schema definition:\n" geni)))
       output)))


(defn process-init-ref [meta rf env]
  (let [nsvec (keyword-split (-> meta :ref :ns))
        data  (nest-keys-in rf nsvec #{:+ :#})]
    (process-init data (-> env :schema :geni) env)))

(defn process-init-assoc [output [meta] v env]
  (cond
   (nil? v) output

   :else
   (let [k (:ident meta)
         t (:type meta)
         v (adjust v meta env)]
     (cond (= t :ref)
           (if (set? v)
             (assoc output k (set (map #(process-init-ref meta % env) v)))
             (assoc output k (process-init-ref meta v env)))

           (or (= t :keyword) (= t :enum))
           (process-assoc-keyword output meta k v)

           :else
           (assoc output k v)))))

(defn process-extras [pdata fgeni merge env]
  (let [nss   (expand-ns-set (get-in pdata [:# :nss]))
        ks    (as/find-keys fgeni nss (-> merge :label) (complement nil?))
        refks (as/find-keys fgeni nss :type :ref)
        dataks     (set (keys pdata))
        mergeks    (clojure.set/difference ks dataks)
        datarefks  (clojure.set/intersection refks dataks)]
    (-> pdata
        ((-> merge :function) fgeni mergeks env)
        (process-extras-current fgeni datarefks merge env))))

(defn process-extras-current
  [pdata fgeni ks merge env]
  (if-let [k (first ks)]
    (let [meta   (-> (fgeni k) first)
          pr-fn  (fn [rf] (process-extras rf fgeni merge env))
          npdata  (if (or (-> env :options :sets-only?)
                         (= :many (:cardinality meta)))
                   (assoc pdata k (set (map pr-fn (pdata k))))
                   (assoc pdata k (pr-fn (pdata k))))]
      (process-extras-current npdata fgeni (next ks) merge env))
    pdata))

(declare characterise
         characterise-nout)

(defn characterise
  ([pdata env] (characterise pdata env {}))
  ([pdata env output]
     (if-let [[k v] (first pdata)]
       (let []
         (characterise (next pdata) env
                       (characterise-nout k v env output)))
       (cond
        (and (nil? (get-in output [:db :id]))
             (-> env :generate :ids env))
        (assoc-in output [:db :id] (iid))

        (and (nil? (get-in output [:# :sym]))
             (-> env :generate :syms env))
        (let [val (if-let [symgen (-> env :generate :sym-gen)]
                    (symgen)
                    (?sym))]
          (assoc-in output [:# :sym] val))
        :else output))))

(defn characterise-nout [k v env output]
  (let [[meta] (-> env :schema :fgeni k)
        t (:type meta)]
    (cond (or (= k :db) (= k :#))
          (assoc output k v)

          (and (set? v) (= :ref t))
          (assoc-in output
                    [:refs-many (-> meta :ref :key)]
                    (set (map #(characterise % env) v)))

          (set? v)
          (assoc-in output [:data-many k] v)

          (= :ref t)
          (assoc-in output
                    [:refs-one (-> meta :ref :key)]
                    (characterise v env))

          :else
          (assoc-in output [:data-one k] v))))

(declare datoms
         datoms-data-one datoms-data-many
         datoms-refs-one datoms-refs-many)

(defn datoms
  "Outputs a datomic structure from characterised result"
  ([chdata]
    (concat (datoms chdata datoms-data-one datoms-data-many)
            (datoms chdata datoms-refs-one datoms-refs-many)))
  ([chdata f1 f2]
    (cond (nil? (seq chdata)) []
        :else
        (concat (mapcat (fn [x] (datoms (second x) f1 f2)) (:refs-one chdata))
                (mapcat (fn [x]
                          (mapcat #(datoms % f1 f2) (second x))) (:refs-many chdata))
                (f1 chdata)
                (f2 chdata)))))

;; Datoms Helper Functions

(defn- datoms-data-one [chd]
  [(assoc (:data-one chd) :db/id (get-in chd [:db :id]))])

(defn- datoms-data-many [chd]
  (for [[k vs] (:data-many chd)
        v vs]
    [:db/add (get-in chd [:db :id]) k v]))

(defn- datoms-refs-one [chd]
  (for [[k ref] (:refs-one chd)]
    [:db/add (get-in chd [:db :id]) k (get-in ref [:db :id])]))

(defn- datoms-refs-many [chd]
  (for [[k refs] (:refs-many chd)
        ref refs]
    [:db/add (get-in chd [:db :id]) k (get-in ref [:db :id])]))

(declare clauses clauses-init
         clauses-data clauses-refs
         clauses-not clauses-fulltext clauses-q)

(defn clauses-sym [chdata] (get-in chdata [:# :sym]))

(defn clauses-pretty-gen [s]
  (let [r (atom 0)]
    (fn []
      (swap! r inc)
      (symbol (str "?" s @r)))))

(defn clauses
  [chdata env]
  (concat [:find (clauses-sym chdata) :where]
          (clauses-init chdata)
          (clauses-not chdata env)
          (clauses-fulltext chdata env)
          (clauses-q chdata)))

(defn clauses-init [chdata]
  (cond
   (nil? (seq chdata)) []
   :else
   (concat  (clauses-data chdata)
            (clauses-refs chdata)
            (mapcat (fn [x]
                      (mapcat clauses-init (second x)))
                    (:refs-many chdata)))))

(defn clauses-data [chdata]
  (let [sym (clauses-sym chdata)]
    (for [[k vs] (:data-many chdata)
           v     vs]
      [sym k v])))

(defn clauses-refs [chdata]
  (let [sym (clauses-sym chdata)]
    (for [[k rs] (:refs-many chdata)
           r     rs]
      [sym k (clauses-sym r)])))

(defn clauses-q [chdata]
  (if-let [chq (get-in chdata [:# :q])] chq []))

(defn clauses-not-gen [ndata sym symgen]
  (let [data  (for [[k vs] (:data-many ndata)
                    v      vs]
                (let [tsym (symgen)]
                  [[sym k tsym]
                   [(list 'not= tsym v)]]))]
    (apply concat data)))

(defn clauses-fulltext-gen [ndata sym symgen]
  (for [[k vs] (:data-many ndata)
            v      vs]
        (let [tsym (symgen)]
          [(list 'fulltext '$ k v) [[sym tsym]]])))

(defn clauses-fn [chdata env gen]
  (if-let [chfn (get-in chdata [:# (:key gen)])]
    (let [symgen (if (-> env :generate :pretty-gen)
                  (clauses-pretty-gen (:prefix gen)) ?sym)
          ndata (-> chfn (process env) (characterise env))
          sym  (clauses-sym chdata)]
      ((:function gen) ndata sym symgen))
    []))

(defn clauses-not [chdata env]
  (clauses-fn chdata env {:key :not
                          :prefix "n"
                          :function clauses-not-gen}))

(defn clauses-fulltext [chdata env]
  (clauses-fn chdata env {:key :fulltext
                          :prefix "ft"
                          :function clauses-fulltext-gen}))
