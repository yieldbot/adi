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
      (recur npdata fgeni (next ks) env))
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
             (recur (next data) geni output)

             (vector? (k geni))
             (recur (next data) geni (assoc output k true))

             (hash-map? (k geni))
             (let [rv (process-make-key-tree v (k geni) {})
                   noutput (assoc output k rv)]
               (recur (next data) geni noutput)))
       output)))

(defn process-find-nss
  ([m] (process-find-nss m #{}))
  ([m output]
     (if-let [[k v] (first m)]
       (cond (hash-map? v)
             (recur (next m) (conj output k))
             :else
             (if-let [ns (keyword-ns k)]
               (recur (next m) (conj output ns))
               (recur (next m) output)))
       output)))

(defn process-make-nss [data geni]
  (-> (process-make-key-tree data geni)
      (flatten-keys-in-keep)
      (process-find-nss)))

(defn process-init-env
  ([geni] (process-init-env geni {}))
  ([geni env]
     (let [schema  (or (:schema env) (as/make-scheme-model geni))
           opts    (or (:options env) {})
           mopts   {:defaults? (if (nil? (:defaults? opts)) true (:defaults? opts))
                    :restrict? (if (nil? (:restrict? opts)) true (:restrict? opts))
                    :required? (if (nil? (:required? opts)) true (:required? opts))
                    :extras? (or (:extras? opts) false)
                    :query? (or (:query? opts) false)
                    :sets-only? (or (:sets-only? opts) false)}]
       (assoc env :schema schema :options mopts))))

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
                 (recur (next data) geni env))

             (hash-map? (geni k))
             (merge (process-init {} v (geni k) env)
                    (process-init output (next data) geni env))

             (not (contains? geni k))
             (if (-> env :options :extras?)
               (recur output (next data) geni env)
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
      (recur npdata fgeni (next ks) merge env))
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
         (recur (next pdata) env
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
    (if (as/ident-reversed? k)
      [:db/add (get-in ref [:db :id]) (as/flip-ident k) (get-in chd [:db :id])]
      [:db/add (get-in chd [:db :id]) k (get-in ref [:db :id])])))

(defn datoms-refs-many [chd]
  (for [[k refs] (:refs-many chd)
        ref refs]
    (if (as/ident-reversed? k)
      [:db/add (get-in ref [:db :id]) (as/flip-ident k) (get-in chd [:db :id])]
      [:db/add (get-in chd [:db :id]) k (get-in ref [:db :id])])))

(defn emit-remove-empty-refs [coll]
  (filter (fn [x]
              (or (vector? x)
                  (and (hash-map? x)
                       (-> (dissoc x :db/id) empty? not))))
          coll))

(defn emit-datoms [data env default-env]
  (cond (or (vector? data) (list? data) (lazy-seq? data))
        (mapcat #(emit-datoms % env default-env) data)

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
        (let [symgen (or (-> env :generate :syms :function) ?sym)
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
    (if (as/ident-reversed? k)
      [(query-sym r) (as/flip-ident k) sym]
      [sym k (query-sym r)]))))

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

(declare deprocess
         deprocess-fm deprocess-view
         deprocess-ref
         deprocess-assoc deprocess-assoc-data deprocess-assoc-ref)

(defn deprocess-init-env [env]
  (if-let [vw (-> env :view)
           ?  (hash-set? vw)
           vw (zipmap vw (repeat :show))]
    (assoc env :view vw)
    env))

(defn deprocess-assoc-data
  [output k v env]
  (let [vw  (or (-> env :view) {})
        dir (or (vw k)
                (-> env :deprocess :data-default)
                :hide)]
    (cond (= dir :show)
          (assoc output k v)

          :else output)))

(defn deprocess-ref
  [k rf meta env exclude]
  (let [vw  (or (-> env :view) {})
        dir (or (deprocess-view-key vw k)
                (-> env :deprocess :refs-default)
                :hide)
        id (:db/id rf)]
    (cond (= dir :hide) nil

          (exclude id) {:+ {:db {:id id}}}

          (= dir :ids) (if id {:+ {:db {:id id}}})

          (= dir :show)
          (let [rns (keyword-split (-> meta :ref :ns))
                nm  (deprocess rf env exclude)
                cm  (get-in nm rns)
                xm  (dissoc-in nm rns)]
            (if (empty? xm) (or cm {})
                (merge cm {:+ xm}))))))

(defn deprocess-assoc-ref
  [output k v meta env exclude]
  (let [c   (or (:cardinality meta) :one)]
    (cond (= c :one)
          (if-let [rout (deprocess-ref k v meta env exclude)]
            (assoc output k rout)
            output)

          (= c :many)
          (assoc output k
                    (-> (map #(deprocess-ref k % meta env exclude) v)
                        (set)
                        (disj nil))))))

(defn deprocess-assoc
  ([output k v meta env exclude]
     (let [t   (:type meta)]
       (cond (not= t :ref)
             (deprocess-assoc-data output k v env)

             :else
             (deprocess-assoc-ref output k v meta env exclude)))))

(defn deprocess-fm
  ([fm env exclude]
     (if-let [id (:db/id fm)]
       (deprocess-fm {:db/id id} fm env (conj exclude id))
       (deprocess-fm {} fm env exclude)))
  ([output fm env exclude]
     (if-let [[k v] (first fm)]
       (if-let [[meta] (-> env :schema :fgeni k)]
         (-> output
             (deprocess-assoc k v meta env exclude)
             (recur (next fm) env exclude))
         (recur output (next fm) env exclude))
       output)))

(defn deprocess-view
  ([fm env exclude vw]
     (deprocess-view {} fm env exclude vw))
  ([output fm env exclude vw]
     (if-let [[k dir] (first vw)]
       (if-let [[meta] (-> env :schema :fgeni k)
                v (or (fm k) (fm (-> meta :ref :key)))]
         (-> output
             (deprocess-assoc k v meta env exclude)
             (recur fm env exclude (next vw)))
         (recur output fm env exclude (next vw)))
       output)))

(defn deprocess
  ([fm env]
     (deprocess fm env #{}))
  ([fm env exclude]
     (let [env (deprocess-init-env env)
           fm-out (deprocess-fm fm env exclude)
           fm-ks  (keys fm-out)
           vw     (apply dissoc (or (-> env :view) {}) fm-ks)
           vw-out (deprocess-view fm env exclude vw)]
       (treeify-keys (merge vw-out fm-out)))))



(declare view)

(defn view-nval
  [fgeni k cfg]
  (if-let [[meta] (fgeni k)
           t      (:type meta)]
    (cond (not= :ref t)
          (or (cfg :data) :show)

          (= :forward (-> meta :ref :type))
          (or (cfg :refs) :ids)

          (= :reverse (-> meta :ref :type))
          (or (cfg :revs) :hide))))

(defn view-loop
  ([fgeni ks cfg]
     (view-loop fgeni ks cfg {}))
  ([fgeni ks cfg output]
     (if-let [k (first ks)]
       (if-let [nval (view-nval fgeni k cfg)]
         (recur fgeni (next ks) cfg (assoc output k nval))
         (recur fgeni (next ks) cfg output))
       output)))

(defn view-keyword [fgeni ns cfg]
  (let [nsks (list-ns-keys fgeni ns)
        ks   (keys (select-keys fgeni nsks))]
    (view-loop fgeni ks cfg)))

(defn view-hashset [fgeni st cfg]
  (apply merge-in (map #(view-keyword fgeni % cfg) st)))

(defn view-hashmap [fgeni m cfg]
  (let [fks (keys fgeni)
        ks  (set (keys (treeify-keys m)))
        st  (view-hashset fgeni ks cfg)]
    (merge st (select-keys (flatten-keys-in m) fks))))

(defn view
  ([fgeni] (view fgeni (list-keyword-ns fgeni)))
  ([fgeni val] (view fgeni val {}))
  ([fgeni val cfg]
     (cond (keyword? val)
           (view-keyword fgeni val cfg)

           (hash-set? val)
           (view-hashset fgeni val cfg)

           (hash-map? val)
           (view-hashmap fgeni val cfg))))

(defn view-make-set [view]
  (->> view
       (filter (fn [[k v]] (= v :show)))
       (map first)
       (set)))









(comment
  (declare deprocess
           deprocess-ref
           deprocess-view
           deprocess-view-data deprocess-view-extras
           deprocess-view-refs deprocess-view-revs)

  (defn deprocess
    ([fm env]
       (let [fgeni (-> env :schema :fgeni)
             nst   (list-keyword-ns fm)
             cfg (or (-> env :deprocess) {})
             vw (view fgeni nst cfg)]
         (deprocess fm vw env)))
    ([fm vw env]
       (deprocess fm vw env #{}))
    ([fm vw env exclude]
       (if-let [id (:db/id fm)]
         (deprocess-view fm vw env (conj exclude id) {:db {:id id}})
         (deprocess-view fm vw env exclude {}))))

  (defn deprocess-ref [rf k dr vw env exclude]
    (cond (= dr :show)
          (let [[meta] (-> env :schema :fgeni k)
                nks (keyword-split (-> meta :ref :ns))
                nm  (deprocess rf vw env exclude)
                cm  (get-in nm nks)
                xm  (dissoc-in nm nks)]
            (if (empty? xm) (or cm {})
                (merge cm {:+ xm})))
          :else
          (if-let [id (:db/id rf)]
            {:+ {:db {:id id}}}
            {})))

  (defn deprocess-view [fm vw env exclude output]
    (let [{:keys [data refs revs]} vw
          d-out (deprocess-view-data fm data)
          rf-out (deprocess-view-refs fm refs vw env exclude)
          rv-out (deprocess-view-revs fm revs vw env exclude)
          extras (if-let [? (-> env :deprocess :extras)
                          fmks (keys fm)
                          eks (clojure.set/difference fmks data refs revs)]
                   (deprocess-view-extras fm eks env exclude))]
      (treeify-keys (merge output d-out rf-out rv-out))))

  (defn deprocess-view-data [fm data-vw]
    (let [ks (->> data-vw
                  (filter (fn [[k v]] (= v :show)))
                  (map first))]
      (select-keys fm ks)))

  (defn deprocess-view-refs
    ([fm refs-vw vw env exclude]
       (deprocess-view-refs fm refs-vw vw env exclude {}))
    ([fm refs-vw vw env exclude output]
       (if-let [[k dr] (first refs-vw)]
         (if-let [?      (not= dr :hide)
                  [meta] (-> env :schema :fgeni k)
                  rf     (fm (-> meta :ref :key))
                  rout   (deprocess-ref rf k dr vw env exclude)]
           (deprocess-view-refs fm (next refs-vw) vw env exclude
                                (assoc output k rout))
           (deprocess-view-refs fm (next refs-vw) vw env exclude output))
         output)))

  (defn deprocess-view-extras
    ([fm ks env exclude]
       (deprocess-view-extras fm ks env exclude {}))
    ([fm ks env exclude output]
       (if-let [k (first ks)]
         (if-let [[meta] (-> env :schema :fgeni k)]
           (cond (= :ref (:type meta)))
           (deprocess-view-extras fm (next ks) env exclude output))
         output)))

  ;; This is the overall method


  (defn emit-path [view]
    (->> (filter (fn [[k v]]
                   (or (= v :ids) (= v :show)))
                 (merge (:refs view) (:revs view)))
         (group-by keyword-nsroot))))


(comment
  (declare deprocess
           deprocess-assoc deprocess-ref)
  (defn deprocess
    ([fm env]
       (let [view (-> (view (-> env :schema :fgeni)
                            (list-keyword-ns fm)))]
         (deprocess fm view env)))
    ([fm view env] (deprocess fm (flatten-keys-in view) env #{}))
    ([fm view env exclude]
       (let [ks   (set (keys view))]
         (if-let [id (:db/id fm)]
           (deprocess fm  view env (conj exclude id) ks {:db {:id id}})
           (deprocess fm view env exclude ks {}))))
    ([fm view env exclude ks output]
       (if-let [k (first ks)]
         (if-let [[meta]  (-> env :schema :fgeni k)
                  v       (or (get fm k) (get fm (-> meta :ref :key)))
                  ps      (view k)
                  add?    (or (= ps :show) (= ps :id))]
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
              (if (empty? xm) (or cm {})
                  (merge cm {:+ xm})))

            :else {})))
  )
