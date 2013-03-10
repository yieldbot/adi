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

(def a (iid))
(hash-map? a)
(type a)

(defn correct-type? [meta v]
  "Checks to see if v matches the description in the meta.
   throws an exception if the v does not"
  (let [t (:type meta)
        c (or (:cardinality meta) :one)
        chk (as/type-checks t)]
    (cond (and (= c :one) (not (chk v)))
          (throw (Exception. (format "The value %s is not of type %s" v t)))
          (and (= c :many) (not (set? v)))
          (throw (Exception. (format "%s needs to be a set" v)))
          (and (= c :many) (not (every? chk v)))
          (throw (Exception. (format "Not every value in %s is not of type %s" v t))))
    true))

(declare correct-value
         correct-value-sets correct-value-normal)

(defn correct-value [meta v opts]
  (let [t     (:type meta)
        c     (or (:cardinality meta) :one)
        chk   (as/type-checks t)]
    (if (:sets-only? opts)
      (correct-value-sets t chk v)
      (correct-value-normal t c chk v))))

(defn- correct-value-sets [t chk v]
  (cond (or (chk v) (= v '_)) #{v}
        (and (set? v) (every? (fn [x] (or (chk x) (= x '_))) v)) v
        :else (throw (Exception. (format "The value/s [%s] are of type %s" v t)))))

(defn- correct-value-normal [t c chk v]
  (cond (= c :one)
        (if (chk v) v
            (throw (Exception. (format "The value %s is not of type %s" v t))))

        (= c :many)
        (cond (and (set? v) (every? chk v)) v
              (chk v) #{v}
              :else (throw (Exception. (format "The value/s [%s] are not of type %s" v t))))))

(declare process
         process-id process-sym process-# process-ref
         process-assoc process-assoc-value process-get-defaults
         process-check-extras process-check-required)


(defn find-key-seq [nskv data]
  (->> [nskv (cons :+ nskv)]
       (filter #(get-in data %))
       first))

(defn find-db-id [val]
  (cond (integer? val) val

         (ref? val)
         (if-let [nskv (find-key-seq [:db :id] val)]
           (get-in val nskv))

         :else (throw (Exception. (str "Not an integer, entity nor hashmap")))))

(defn process
    "Processes the data according to the schema specified to form a tree-like
   structure of refs and values for the next step of characterisation."
    ([data m] (process data m {}))
    ([data m opts]
       (let [mopts {:fschm (or (:fschm opts) (flatten-all-keys m))
                    :nss (or (:nss opts) (set (keys (treeify-all-keys m))))
                    :defaults? (if (nil? (:defaults? opts)) true (:defaults? opts))
                    :required? (or (:required? opts) false)
                    :extras? (or (:extras? opts) true)
                    :sets-only? (or (:sets-only? opts) false)}]
         (process (treeify-all-keys data) (:fschm mopts) mopts {})))
    ([data fschm opts output]
      ;;(println data)
      (if-let [[k [meta]] (first fschm)]
        (let [kt  (find-key-seq (k-unmerge k) data)]
          (->> output
               (process-assoc data meta k opts)
               (process (if kt (dissoc-in-keepempty data kt) data) (rest fschm) opts)))
        (->> output
             (process-id data)
             (apply process-#)
             (apply process-check-extras opts)
             (process-check-required opts)))))

(defn process-assoc [data meta k opts output]
  (let [kt     (find-key-seq (k-unmerge k) data)
        v      (or (if kt (get-in data kt))
                   (process-get-defaults data meta k opts))]
    (cond
      (nil? v) output

      :else
      (let [v (correct-value meta v opts)
            t (:type meta)]
        (cond (not= t :ref)
              (cond (and (:sets-only? opts) (not (set? v)))
                    (assoc output k #{v})
                    :else
                    (assoc output k v))

              :else
              (cond (and (:sets-only? opts) (not (set? v)))
                    (assoc output k (hash-set (process-ref meta v opts)))

                    (set? v)
                    (assoc output k (set (map #(process-ref meta % opts) v)))

                    :else
                    (assoc output k (process-ref meta v opts))))))))

(defn- process-ref [meta v opts]
  (let [nsv   (k-unmerge (:ref-ns meta))
        data  (extend-keys (treeify-keys v) nsv #{:+ :#})]
    (process data (:fschm opts) opts)))

(defn- process-get-defaults [data meta k opts]
  (if (:defaults? opts)
    (let [nss (or (:nss opts) #{})
          nv  (k-nsv k)]
      (if (and (get-in data nv) (nss (first nv)))
        (:default meta)))))

(defn- process-check-extras [opts data output]
  (if (or (:extras? opts) (empty? (flatten-all-keys (remove-all-keys data [:+ :#]))))
    output
    (throw (Exception. (str "There are unprocessed data: " data output opts)))))

(defn- process-check-required [opts output]
  (if (:required? opts)
    (let [fschm (:fschm opts)
          ks  (set (keys output))
          nss (set (map k-ns ks))
          rks (as/required-keys fschm nss)]
      (doseq [rk rks]
        (if (not (nss rk))
          (throw (Exception. (str rk "is a required key in " output)))))
      (doseq [m (filter hash-map? (vals output))]
        (process-check-required opts m))))
  output)

(defn- process-id [data output]
  ;;(println data)
  (if-let [idv (find-key-seq [:db :id] data)]
    (let [id  (get-in data idv)]
      ;;(println id idv)
      [(dissoc-in data idv) (assoc output :db/id id)])
    [data output]))

(defn- process-# [data output]
  (let [fn-m (select-keys data [:#])]
    [(dissoc data :#) (merge output fn-m)]))


(declare unprocess
         unprocess-assoc unprocess-ref)

(defn unprocess
  "The opposite of process. Takes a map or an entity and turns it into a nicer looking
   data-structure"
  ([fschm pdata rrs]
     (if-let [id (:db/id pdata)]
       (unprocess fschm pdata rrs #{id} {:db/id id})
       (unprocess fschm pdata rrs #{} {})))
  ([fschm pdata rrs exclude]
     (unprocess fschm rrs pdata exclude {}))
  ([fschm pdata rrs exclude output]
     (if-let [[k v] (first pdata)]
       (if-let [[meta] (k fschm)]
         (->> output
             (unprocess-assoc fschm rrs meta k v exclude)
             (unprocess fschm (next pdata) rrs exclude))
         (unprocess fschm (next pdata) rrs output exclude))
       output)))

(defn- unprocess-assoc [fschm rrs meta k v exclude output]
  (if (correct-type? meta v)
    (let [t (:type meta)
          c (or (:cardinality meta) :one)
          kns (k-unmerge k)]
      (cond (not= t :ref)
            (assoc-in output kns v)

            (= c :one)
            (assoc-in output kns (unprocess-ref fschm rrs meta k v exclude))

            (= c :many)
            (assoc-in output kns
                      (set (map #(unprocess-ref fschm rrs meta k % exclude) v)))))))

(defn- unprocess-ref [fschm rrs meta k v exclude]
  (let [id (:db/id v)]
      (cond (exclude id)
            {:+ {:db/id id}}

            (get rrs k)
            (let [nks (k-unmerge (:ref-ns meta))
                  nm  (unprocess fschm v rrs)
                  cm  (get-in nm nks)
                  xm  (dissoc-in nm nks)]
              (if (empty? xm) cm
                  (merge cm (assoc {} :+ xm))))

            :else
            (if id {:+ {:db/id id}} {}))))

(defn pretty-gen [s]
  (let [r (atom 0)]
    (fn []
      (swap! r inc)
      (symbol (str "?" s @r)))))

(defn characterise
  "Characterises the data into datomic specific format so that converting
   into datomic datastructures become easy."
  ([pdata fschm] (characterise pdata fschm {}))
  ([pdata fschm opts] (characterise pdata fschm opts {}))
  ([pdata fschm opts output]
     (if-let [[k v] (first pdata)]
       (let [t (-> fschm k first :type)]
         (cond (or (= k :db/id) (= k :#))
               (characterise (next pdata) fschm opts (assoc output k v))

               (and (set? v) (= :ref t))
               (characterise (next pdata) fschm opts
                             (assoc-in output
                                       [:refs-many k]
                                       (set (map #(characterise % fschm opts) v))))

               (set? v)
               (characterise (next pdata) fschm opts (assoc-in output [:data-many k] v))

               (= :ref t)
               (characterise (next pdata) fschm opts
                             (assoc-in output
                                       [:refs-one k]
                                       (characterise v fschm opts)))

               :else
               (characterise (next pdata) fschm opts (assoc-in output [:data-one k] v))))
       (cond
        (and (nil? (:db/id output)) (:generate-ids opts))
        (assoc output :db/id (iid))

        (and (nil? (get-in output [:# :sym])) (:generate-syms opts))
        (assoc-in output [:# :sym] (if (:p-gen opts) ((:p-gen opts)) (?sym)))

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
  [(assoc (:data-one chd) :db/id (:db/id chd))])

(defn- build-data-many [chd]
  (for [[k vs] (:data-many chd)
        v vs]
    [:db/add (:db/id chd) k v]))

(defn- build-refs-one [chd]
  (for [[k ref] (:refs-one chd)]
    [:db/add (:db/id chd) k (:db/id ref)]))

(defn- build-refs-many [chd]
  (for [[k refs] (:refs-many chd)
        ref refs]
    [:db/add (:db/id chd) k (:db/id ref)]))



(declare clauses
         clauses-data clauses-refs
         clauses-not clauses-fulltext clauses-q)

(defn get-sym [chdata] (get-in chdata [:# :sym]))

(defn build-query [chdata fschm & [pretty]]
  (concat [:find (get-sym chdata) :where]
          (clauses chdata)
          (clauses-not chdata fschm pretty)
          (clauses-fulltext chdata fschm pretty)
          (clauses-q chdata)))

(defn clauses
  "Builds the datomic query structure from the
  characterised result"
  ([chdata]
     (cond
      (nil? (seq chdata)) []
      :else
      (concat  (clauses-data chdata)
               (clauses-refs chdata)
               (mapcat (fn [x]
                         (mapcat clauses (second x))) (:refs-many chdata))))))

(defn- clauses-data [chdata]
  (let [sym (get-sym chdata)]
    (for [[k vs] (:data-many chdata)
           v     vs]
      [sym k v])))

(defn- clauses-refs [chdata]
  (let [sym (get-sym chdata)]
    (for [[k rs] (:refs-many chdata)
           r     rs]
      [sym k (get-in r [:# :sym]) ])))

(defn clauses-not [chdata fschm & [pretty]]
  (if-let [chdn (get-in chdata [:# :not])]
    (let [p-gen (if pretty (pretty-gen "ng") ?sym)
          ndata (-> (process chdn fschm {:sets-only? true
                                         :defaults? false})
                    (characterise fschm {:generate-syms true}))
          sym  (get-sym chdata)]
      (apply concat
             (for [[k vs] (:data-many ndata)
                   v      vs]
               (let [tsym (p-gen)]
                 [[sym k tsym]
                  [(list 'not= tsym v)]])
               )))
    []))

(defn clauses-fulltext [chdata fschm & [pretty]]
  (if-let [chdn (get-in chdata [:# :fulltext])]
    (let [p-gen (if pretty (pretty-gen "ft") ?sym)
          ndata (-> (process chdn fschm {:sets-only? true
                                         :defaults? false})
                    (characterise fschm {:generate-syms true}))
          sym  (get-sym chdata)]
      (for [[k vs] (:data-many ndata)
            v      vs]
        (let [tsym (p-gen)]
          [(list 'fulltext '$ k v) [[sym tsym]]])))
    []))

(defn clauses-q [chdata]
  (if-let [chdn (get-in chdata [:# :q])] chdn []))
