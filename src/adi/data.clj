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
      (adjust-value-sets-only v chk env err-many)
      (adjust-value-normal v meta chk env err-one err-many)))

(defn adjust-safe-check [chk v env]
  (or (try (chk v) (catch Exception e))
      (= v '_)
      (and (-> env :options :query?) (vector? v))))

(defn adjust-value-sets-only [v chk env err-many]
  (cond (adjust-safe-check chk v env) #{v}
        (and (set? v) (every? #(adjust-safe-check chk % env) v)) v
        :else (throw (Exception. err-many))))

(defn adjust-value-normal [v meta chk env err-one err-many]
  (let [c (or (:cardinality meta) :one)]
    (cond (= c :one)
          (if (adjust-safe-check chk v env) v
              (throw (Exception. err-one)))

          (= c :many)
          (cond (adjust-safe-check chk v env) #{v}
                (and (set? v) (every? #(adjust-safe-check chk % env) v)) v
                :else (throw (Exception. err-many))))))

(declare process process-assoc-keyword
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

(defn process-init-env [geni env]
  (let [schema  (or (:schema env) (as/make-scheme-model geni))
        opts    (or (:options env) {})
        mopts   {:defaults? (if (nil? (:defaults? opts)) true (:defaults? opts))
                 :restrict? (if (nil? (:restrict? opts)) true (:restrict? opts))
                 :required? (if (nil? (:required? opts)) true (:required? opts))
                 :extras? (or (:extras? opts) false)
                 :query? (or (:query? opts) false)
                 :sets-only? (or (:sets-only? opts) false)}]
    (assoc env :schema schema :options mopts)))

(defn process-assoc-keyword
  ([output meta k v]
     (let [t   (:type meta)
           kns (-> meta t :ns)]
       (cond (set? v)
             (assoc output k (set (map #(keyword-join [kns %]) v)))

             :else
             (assoc output k (keyword-join [kns v]))))))

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

(defn characterise-gen-id [output env]
  (if-let [no-id? (nil? (get-in output [:db :id]))
           gen    (-> env :generate :ids)]
    (let [idgen (or (:function gen) iid)]
      (assoc-in output [:db :id] (idgen)))
    output))

(defn characterise-gen-sym [output env]
  (if-let [no-sym? (nil? (get-in output [:# :sym]))
           gen     (-> env :generate :syms)]
    (let [symgen (or (:function gen) ?sym)]
      (assoc-in output [:# :sym] (symgen)))
    output))

(defn characterise
  ([pdata env]
     (characterise pdata env
                   (-> (characterise-gen-id {} env)
                       (characterise-gen-sym env))))
  ([pdata env output]
     (if-let [[k v] (first pdata)]
       (let []
         (characterise (next pdata) env
                       (characterise-nout k v env output)))
       output)))

(defn characterise-nout [k v env output]
  (if-let [[meta] (-> env :schema :fgeni k)
        t (:type meta)]
    (cond
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
          (assoc-in output [:data-one k] v))

    (cond (= k :db)
          (assoc output k v)

          (= k :#)
          (assoc output k (merge-in (output k) v))

          :else
          (error "key " k " not found in schema."))))

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
        (concat (mapcat (fn [x] (datoms (second x) f1 f2))
                        (:refs-one chdata))
                (mapcat (fn [x]
                          (mapcat #(datoms % f1 f2) (second x)))
                        (:refs-many chdata))
                (f1 chdata)
                (f2 chdata)))))

;; Datoms Helper Functions

(defn datoms-data-one [chd]
  [(assoc (:data-one chd) :db/id (get-in chd [:db :id]))])

(defn datoms-data-many [chd]
  (for [[k vs] (:data-many chd)
        v vs]
    [:db/add (get-in chd [:db :id]) k v]))

(defn datoms-refs-one [chd]
  (for [[k ref] (:refs-one chd)]
    [:db/add (get-in chd [:db :id]) k (get-in ref [:db :id])]))

(defn datoms-refs-many [chd]
  (for [[k refs] (:refs-many chd)
        ref refs]
    [:db/add (get-in chd [:db :id]) k (get-in ref [:db :id])]))

(defn emit-remove-empty-refs [coll]
  (filter (fn [x]
              (or (vector? x)
                  (and (hash-map? x)
                       (-> (dissoc x :db/id) empty? not))))
          coll))

(defn emit-datoms [data env default-env]
  (cond (or (vector? data) (list? data) (lazy-seq? data))
        (mapcat #(emit-datoms % env) data)

        (hash-map? data)
        (let [menv (merge-in env default-env)
              chdata  (-> data
                          (process menv)
                          (characterise menv))]
          (emit-remove-empty-refs (datoms chdata)))))

(defn emit-datoms-insert [data env]
  (emit-datoms data env {:generate {:ids {:current true}}}))

(defn emit-datoms-update [data env]
  (emit-datoms data env {:options {:required? false
                                   :extras? true
                                   :defaults? false}}))

(defn make-?? [f args]
  [(apply list 'list
          (concat [(list 'quote f)
                   (list 'symbol "??")]
                  `[~@args]))])

(defmacro ?? [f & args]
  (make-?? f args))

(defmacro ? [f & args]
  [[(list 'symbol "??sym")
    (list 'symbol "??attr")
    (list 'symbol "??")]
   (make-?? f args)])

(defn ?not [val]
  (? not= val))

(defn ?fulltext [val]
  [[(list 'fulltext '$ '??attr val) [['??sym '??]]]])


(declare query query-init
         query-data query-refs
         query-not query-fulltext query-q)

(defn query-sym [chdata] (get-in chdata [:# :sym]))

(defn query
  [chdata env]
  (vec (concat [:find (query-sym chdata) :where]
               (query-init chdata env)
               (query-q chdata))))

(defn query-init [chdata env]
  (cond
   (nil? (seq chdata)) []
   :else
   (concat  (query-data chdata env)
            (query-refs chdata)
            (mapcat (fn [x]
                      (mapcat #(query-init % env) (second x)))
                    (:refs-many chdata)))))

(defn query-data-val [sym k v env]
  (cond (vector? v)
        (let [symgen (-> env :generate :syms :function)
              esym (symgen)]
          (replace-walk v {'??sym sym '?? esym '??attr k}))
        :else
        [[sym k v]]))

(defn query-data [chdata env]
  (let [sym  (query-sym chdata)
        data (for [[k vs] (:data-many chdata)
                   v     vs]
               (query-data-val sym k v env))]
    (apply concat data)))

(defn query-refs [chdata]
  (let [sym (query-sym chdata)]
    (for [[k rs] (:refs-many chdata)
           r     rs]
      [sym k (query-sym r)])))

(defn query-q [chdata]
  (if-let [chq (get-in chdata [:# :q])] chq []))

(defn emit-query [data env]
  (let [menv (merge-in env {:options {:restrict? false
                                      :required? false
                                      :defaults? false
                                      :sets-only? true
                                      :query? true}
                            :generate {:syms {:current true}}})
        chdata  (-> data
                    (process menv)
                    (characterise menv))]
    (query chdata menv)))


(defn emit-view
  ([geni] (emit-view geni (set (keys geni))))
  ([geni val] (emit-view geni val {}))
  ([geni val cfg]
     (let [cfg (merge {:data :show :ref :id :rev-refs false} cfg)]
       (cond (keyword? val)
             (let [ks (-> (select-keys geni [val])
                          flatten-keys-in)
                   dks (as/find-keys ks :type (fn [t] (not= :ref t)))
                   rks (as/find-keys ks (constantly true)
                                     :type :ref
                                     :ref (if (cfg :rev-refs)
                                            (constantly true)
                                            (fn [m] (= :forward (:type m)))))]
               (-> (merge (zipmap dks (repeat (cfg :data)))
                          (zipmap rks (repeat (cfg :ref))))
                   treeify-keys))


             (hash-set? val)
             (apply merge (map #(emit-view geni % cfg) val))

             (hash-map? val)
             (let [nval (treeify-keys val)]
               (merge-in (emit-view geni (set (keys nval)) cfg)
                         nval))))))

(declare deprocess
         deprocess-assoc deprocess-ref)

(defn deprocess
  ([fm env]
     (let [view (-> (emit-view (-> env :schema :geni)
                               (list-keyword-ns fm))
                    flatten-keys-in)]
       (deprocess fm view env)))
  ([fm view env] (deprocess fm view env #{}))
  ([fm view env exclude]
     (let [ks   (clojure.set/union (set (keys fm)) (set (keys view)))]
       (if-let [id (fm :db/id)]
         (deprocess (dissoc fm :db/id) view env (conj exclude id) ks {:db {:id id}})
         (deprocess fm view env exclude ks {}))))
  ([fm view env exclude ks output]
     (if-let [k (first ks)]
       (if-let [[meta]  (-> env :schema :fgeni k)
                v       (or (fm k) (fm (-> meta :ref :key)))
                ps      (view k)
                add?    (or (= ps :show) (= ps :id)
                            (and (-> env :deprocess :show?)
                                 (not= :hide ps)))]
         (->> output
              (deprocess-assoc k v view env exclude)
              (deprocess fm view env exclude (next ks)))
         (deprocess fm view env exclude (next ks) output))
       output)))

(defn deprocess-assoc [k v view env exclude output]
  (if-let [[meta]  (-> env :schema :fgeni k)
           kns     (keyword-split k)]
    (cond (not= (:type meta) :ref)
          (assoc-in output kns v)

          (hash-set? v)
          (assoc-in output kns
                    (set (map #(deprocess-ref k % view env exclude) v)))

          :else
          (assoc-in output kns (deprocess-ref k v view env exclude)))
    output))

(defn deprocess-ref [k ref view env exclude]
  (let [id      (:db/id ref)
        [meta]  (-> env :schema :fgeni k)
        ps      (get view k)]
    (cond (and id (or (= :id ps) (exclude id)))
          {:+ {:db {:id id}}}

          (= :show ps)
          (let [nks (keyword-split (-> meta :ref :ns))
                nm  (deprocess ref view env exclude)
                cm  (get-in nm nks)
                xm  (dissoc-in nm nks)]
            (if (empty? xm) cm
                (merge cm {:+ xm})))

          :else {})))
