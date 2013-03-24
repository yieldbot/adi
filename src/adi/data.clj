(ns adi.data
  (:use [datomic.api :only [tempid]]
        [adi.schema :as as]
        adi.utils))

(defn iid
  "Constructs a new id"
  ([] (tempid :db.part/user))
  ([obj]
     (let [v (hash obj)
           v (if (< 0 v) (- v) v)]
       (tempid :db.part/user v ))))


(declare adjust adjust-restrict
         adjust-chk adjust-sets-only adjust-normal)

(defn adjust [v meta opts]
  (let [chk    (as/geni-type-checks (:type meta))
        rchk   (:restrict meta)
        err-one (format "The value %s is not of type %s, it is of type %s"
                        v (:type meta) (type v))
        err-many (format "The value/s [%s] are not of type %s"
                         v meta)
        output  (if (:sets-only? opts)
                  (adjust-sets-only v meta chk err-many)
                  (adjust-normal v meta chk err-one err-many))]
    (if rchk
      (adjust-restrict output rchk meta opts)
      output)))

(defn- adjust-restrict [v rchk meta opts]
  (let [rerr-one  (format "The value %s does not meet the restriction %s"
                         v rchk)
        rerr-many (format "The value/s [%s] do not meet the restriction %s"
                          v rchk)]
    (if (:sets-only? opts)
      (adjust-sets-only v meta rchk rerr-many)
      (adjust-normal v meta rchk rerr-one rerr-many))))

(defn- adjust-chk [chk v]
  (or (try (chk v) (catch Exception e)) (= v '_)))

(defn- adjust-sets-only [v meta chk err-many]
  (cond (adjust-chk chk v) #{v}
        (and (set? v) (every? #(adjust-chk chk %) v)) v
        :else (throw (Exception. err-many))))

(defn- adjust-normal [v meta chk err-one err-many]
  (let [c (or (:cardinality meta) :one)]
    (cond (= c :one)
          (if (adjust-chk chk v) v
              (throw (Exception. err-one)))

          (= c :many)
          (cond (adjust-chk chk v) #{v}
                (and (set? v) (every? #(adjust-chk chk %) v)) v
                :else (throw
                       (Exception. err-many ))))))


(declare process
         process-nss-fn process-nss-plus-fn
         process-init process-init-assoc process-init-ref
         process-defaults process-defaults-merge process-defaults-ref
         process-required process-required-merge process-required-ref)

(defn process-nss-plus-fn [data]
  (if-let [+v (data :+)]
    (merge (dissoc data :+)
           (process-nss-plus-fn +v))
    data))

(defn process-nss-fn [data]
  (let [rm-ex (fn [s] (disj s :# :db))]
    (-> data
        treeify-all-keys
        process-nss-plus-fn
        keys
        set
        rm-ex)))

(defn process
  ([data geni] (process data geni {}))
  ([data geni opts]
     (let [mopts {:geni  (or (:geni opts) geni)
                  :fgeni (or (:fgeni opts) (flatten-all-keys geni))
                  :defaults? (if (nil? (:defaults? opts)) true (:defaults? opts))
                  :required? (or (:required? opts) false)
                  :extras? (or (:extras? opts) false)
                  :sets-only? (or (:sets-only? opts) false)}]
       (-> (process-init data geni mopts)
           (process-defaults mopts)
           (process-required mopts)))))

(defn process-init
  ([data geni opts]
     (let [nss (process-nss-fn data)]
       (-> (process-init {} (treeify-all-keys data) geni
                         (assoc opts :nss nss))
           (assoc-in [:# :nss] nss))))

  ([output data geni opts]
     (if-let [[k v] (first data)]
       (cond (= k :+)
             (merge (process-init {} v (opts :geni) opts)
                    (process-init output (next data) geni opts))

             (or (= k :#) (= k :db))
             (assoc (process-init output (next data) geni opts) k v)

             (not (contains? geni k))
             (if (:extras? opts)
               (process-init output (next data) geni opts)
               (throw (Exception.
                       (format "Data not found in schema definition (%s %s), %s"
                               k v geni))))
             (vector? (geni k))
             (-> (process-init-assoc output (first (geni k)) v opts)
                 (process-init (next data) geni opts))

             (hash-map? (geni k))
             (merge (process-init {} v (geni k) opts)
                    (process-init output (next data) geni opts)))
       output)))

(defn process-keyword-assoc [output meta k v]
  (let [kns (:keyword-ns meta)]
    (cond (set? v)
          (assoc output k (set (map #(key-merge [kns %]) v)))

          :else
          (assoc output k (key-merge [kns v])))))

(defn process-init-assoc [output meta v opts]
  (cond
   (nil? v) output

   :else
   (let [k (:ident meta)
         t (:type meta)
         v (adjust v meta opts)]
     (cond (= t :ref)
           (cond (set? v)
                 (assoc output k (set (map #(process-init-ref meta % opts) v)))

                 :else
                 (assoc output k (process-init-ref meta v opts)))

           (and (= t :keyword) (:keyword-ns meta))
           (process-keyword-assoc output meta k v)

           :else
           (assoc output k v)))))

(defn- process-init-ref [meta v opts]
  (let [nsvec (key-unmerge (:ref-ns meta))
        data  (extend-keys v nsvec #{:+ :#})]
    (process-init data (:geni opts)
                  (assoc opts :nss (process-nss-fn v)))))

(defn process-defaults [idata opts & [nss-ex]]
  (if (:defaults? opts)
    (let [nss (or (get-in idata [:# :nss]) (list-key-ns idata))
          nss (if (nil? nss-ex) nss (conj nss nss-ex))
          fgeni (:fgeni opts)
          ks (as/find-default-keys fgeni nss)
          refks (as/find-ref-keys fgeni nss)
          dataks  (set (keys idata))
          mergeks  (clojure.set/difference ks dataks)
          assocks  (clojure.set/intersection refks dataks)]
      (-> idata
          (process-defaults-merge fgeni mergeks opts)
          (process-defaults-ref   fgeni assocks opts)))
    idata))

(defn- process-defaults-merge
  [idata fgeni mergeks opts]
  (if-let [k (first mergeks)]
    (let [meta   (-> (fgeni k) first)
          t      (:type meta)
          isset? (or (:sets-only? opts)
                     (= :many (:cardinality meta)))
          dft    (:default meta)
          dft    (if (fn? dft) (dft) dft)
          dft    (if (and isset? (not (set? dft))) #{dft} dft)]
      (cond
       (and (= t :keyword) (:keyword-ns meta))
       (process-defaults-merge (process-keyword-assoc idata meta k dft)
                              fgeni (next mergeks) opts)

       :else
       (process-defaults-merge (assoc idata k dft)
                               fgeni (next mergeks) opts)))
    idata))

(defn- process-defaults-ref
  [idata fgeni assocks opts]
  (if-let [k (first assocks)]
    (let [meta   (-> (fgeni k) first)
          t      (:type meta)
          isset? (or (:sets-only? opts)
                     (= :many (:cardinality meta)))
          ref-ns (:ref-ns meta)
          pd-fn  (fn [rf] (process-defaults rf opts ref-ns))]
      (if isset?
        (assoc idata k (set (map pd-fn (idata k))))
        (assoc idata k (pd-fn (idata k)))))
    idata))

(defn process-required [idata opts & [nss-ex]]
  (if (:required? opts)
    (let [nss (or (get-in idata [:# :nss]) (list-key-ns idata))
          nss (if (nil? nss-ex) nss (conj nss nss-ex))
          fgeni (:fgeni opts)
          ks (as/find-required-keys fgeni nss)
          dataks   (set (keys idata))
          mergeks  (clojure.set/difference ks dataks)
          refks    (as/find-ref-keys fgeni nss)
          assocks  (clojure.set/intersection refks dataks)]
      (-> idata
          (process-required-merge fgeni mergeks)
          (process-required-ref   fgeni assocks opts)))
    idata))

(defn- process-required-merge
  [idata fgeni mergeks]
  (if (empty? mergeks) idata
    (throw (Exception. (str "The following keys are required: " mergeks)))))

(defn- process-required-ref
  [idata fgeni assocks opts]
  (if-let [k (first assocks)]
    (let [meta   (-> (fgeni k) first)
          t      (:type meta)
          isset? (or (:sets-only? opts)
                     (= :many (:cardinality meta)))
          ref-ns (:ref-ns meta)
          pr-fn  (fn [rf] (process-required rf opts ref-ns))]
      (if isset?
        (assoc idata k (set (map pr-fn (idata k))))
        (assoc idata k (pr-fn (idata k)))))
    idata))

(defn characterise
  "Characterises the data into datomic specific format so that converting
   into datomic datastructures become easy."
  ([pdata fgeni] (characterise pdata fgeni {}))
  ([pdata fgeni opts] (characterise pdata fgeni opts {}))
  ([pdata fgeni opts output]
     (if-let [[k v] (first pdata)]
       (let [t (-> fgeni k first :type)]
         (cond (or (= k :db) (= k :#))
               (characterise (next pdata) fgeni opts (assoc output k v))

               (and (set? v) (= :ref t))
               (characterise (next pdata) fgeni opts
                             (assoc-in output
                                       [:refs-many k]
                                       (set (map #(characterise % fgeni opts) v))))

               (set? v)
               (characterise (next pdata) fgeni opts (assoc-in output [:data-many k] v))

               (= :ref t)
               (characterise (next pdata) fgeni opts
                             (assoc-in output
                                       [:refs-one k]
                                       (characterise v fgeni opts)))

               :else
               (characterise (next pdata) fgeni opts (assoc-in output [:data-one k] v))))
       (cond
        (and (nil? (get-in output [:db :id])) (:generate-ids opts))
        (assoc-in output [:db :id] (iid))

        (and (nil? (get-in output [:# :sym])) (:generate-syms opts))
        (assoc-in output [:# :sym] (if-let [symgen (:sym-gen opts)] (symgen) (?sym)))

        :else output))))

(declare build
         build-data-one build-data-many
         build-refs-one build-refs-many)

(defn build
  "Builds the datomic query structure from the
  characterised result"
  ([chdata]
    (concat (build chdata build-data-one build-data-many)
            (build chdata build-refs-one build-refs-many)))
  ([chdata f1 f2]
    (cond (nil? (seq chdata)) []
        :else
        (concat (mapcat (fn [x] (build (second x) f1 f2)) (:refs-one chdata))
                (mapcat (fn [x]
                          (mapcat #(build % f1 f2) (second x))) (:refs-many chdata))
                (f1 chdata)
                (f2 chdata)))))

;; Build Helper Functions

(defn- build-data-one [chd]
  [(assoc (:data-one chd) :db/id (get-in chd [:db :id]))])

(defn- build-data-many [chd]
  (for [[k vs] (:data-many chd)
        v vs]
    [:db/add (get-in chd [:db :id]) k v]))

(defn- build-refs-one [chd]
  (for [[k ref] (:refs-one chd)]
    [:db/add (get-in chd [:db :id]) k (get-in ref [:db :id])]))

(defn- build-refs-many [chd]
  (for [[k refs] (:refs-many chd)
        ref refs]
    [:db/add (get-in chd [:db :id]) k (get-in ref [:db :id])]))


;; Clauses


(declare clauses clauses-init
         clauses-data clauses-refs
         clauses-not clauses-fulltext clauses-q)

(defn clauses-sym [chdata] (get-in chdata [:# :sym]))
(defn clauses-pretty-gen [s]
  (let [r (atom 0)]
    (fn []
      (swap! r inc)
      (symbol (str "?" s @r)))))

(defn clauses [chdata fgeni & [pretty]]
  (concat [:find (clauses-sym chdata) :where]
          (clauses-init chdata)
          (clauses-not chdata fgeni pretty)
          (clauses-fulltext chdata fgeni pretty)
          (clauses-q chdata)))

(defn clauses-init
  "Builds the datomic query structure from the
  characterised result"
  ([chdata]
     (cond
      (nil? (seq chdata)) []
      :else
      (concat  (clauses-data chdata)
               (clauses-refs chdata)
               (mapcat (fn [x]
                         (mapcat clauses-init (second x))) (:refs-many chdata))))))

(defn- clauses-data [chdata]
  (let [sym (clauses-sym chdata)]
    (for [[k vs] (:data-many chdata)
           v     vs]
      [sym k v])))

(defn- clauses-refs [chdata]
  (let [sym (clauses-sym chdata)]
    (for [[k rs] (:refs-many chdata)
           r     rs]
      [sym k (get-in r [:# :sym]) ])))

(defn clauses-not [chdata fgeni & [pretty]]
  (if-let [chdn (get-in chdata [:# :not])]
    (let [p-gen (if pretty (clauses-pretty-gen "ng") ?sym)
          ndata (-> (process chdn fgeni {:sets-only? true
                                         :defaults? false})
                    (characterise fgeni {:generate-syms true}))
          sym  (clauses-sym chdata)]
      (apply concat
             (for [[k vs] (:data-many ndata)
                   v      vs]
               (let [tsym (p-gen)]
                 [[sym k tsym]
                  [(list 'not= tsym v)]])
               )))
    []))

(defn clauses-fulltext [chdata fgeni & [pretty]]
  (if-let [chdn (get-in chdata [:# :fulltext])]
    (let [p-gen (if pretty (clauses-pretty-gen "ft") ?sym)
          ndata (-> (process chdn fgeni {:sets-only? true
                                         :defaults? false})
                    (characterise fgeni {:generate-syms true}))
          sym  (clauses-sym chdata)]
      (for [[k vs] (:data-many ndata)
            v      vs]
        (let [tsym (p-gen)]
          [(list 'fulltext '$ k v) [[sym tsym]]])))
    []))

(defn clauses-q [chdata]
  (if-let [chdn (get-in chdata [:# :q])] chdn []))
