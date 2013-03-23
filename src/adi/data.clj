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


(declare adjust
         adjust-chk adjust-sets-only adjust-normal)

(defn adjust [meta v opts]
    (let [chk   (as/geni-type-checks (:type meta))]
      (if (:sets-only? opts)
        (adjust-sets-only meta chk v)
        (adjust-normal meta chk v))))

(defn- adjust-chk [chk v]
  (or (chk v) (= v '_)))

(defn- adjust-sets-only [meta chk v]
  (cond (adjust-chk chk v) #{v}
        (and (set? v) (every? #(adjust-chk chk %) v)) v
        :else (throw
               (Exception.
                (format "The value/s [%s] are of type %s, %s" v (:type meta) meta)))))

(defn- adjust-normal [meta chk v]
  (let [c (or (:cardinality meta) :one)]
    (cond (= c :one)
          (if (adjust-chk chk v) v
              (throw (Exception.
                      (format "The value %s is not of type %s, it is of type %s" v (:type meta) (type v)))))

          (= c :many)
          (cond (adjust-chk chk v) #{v}
                (and (set? v) (every? #(adjust-chk chk %) v)) v
                :else (throw
                       (Exception.
                        (format "The value/s [%s] are not of type %s" v meta)))))))


(declare process
         process-init process-init-assoc process-init-ref
         process-defaults process-defaults-merge process-defaults-ref
         process-required process-required-merge process-required-ref)

(defn process
  ([data geni] (process data geni {}))
  ([data geni opts]
     (let [mopts {:geni (or (:geni opts) geni)
                  :fgeni (or (:fgeni opts) (flatten-all-keys geni))
                  :nss (or (:nss opts) (set (keys geni)))
                  :defaults? (if (nil? (:defaults? opts)) true (:defaults? opts))
                  :required? (or (:required? opts) false)
                  :extras? (or (:extras? opts) false)
                  :sets-only? (or (:sets-only? opts) false)}]
       (-> (process-init data geni mopts)
           (process-defaults mopts)
           (process-required mopts)))))

(defn process-init
  ([data geni opts] (process-init {} (treeify-all-keys data) geni opts))
  ([output data geni opts]
     (if-let [[k v] (first data)]
       (cond (= k :+)
             (merge (process-init {} v (opts :geni) opts)
                    (process-init output (next data) geni opts))

             (= k :#)
             (assoc (process-init output (next data) geni opts) k v)

             (not (contains? geni k))
             (if (:extras? opts)
               (process-init output (next data) geni opts)
               (throw (Exception.
                       (format "Data not found in schema definition (%s %s), %s" k v geni))))
             (vector? (geni k))
             (-> (process-init-assoc output (first (geni k)) v opts)
                 (process-init (next data) geni opts))

             (hash-map? (geni k))
             (merge (process-init {} v (geni k) opts)
                    (process-init output (next data) geni opts)))
       output)))


(defn process-init-assoc [output meta v opts]
  (cond
   (nil? v) output

   :else
   (let [k (:ident meta)
         t (:type meta)
         v (adjust meta v opts)]
     (cond (= t :ref)
           (cond (set? v)
                 (assoc output k (set (map #(process-init-ref meta % opts) v)))

                 :else
                 (assoc output k (process-init-ref meta v opts)))

           (and (= t :keyword) (:keyword-ns meta))
           (let [kns (:keyword-ns meta)]
               (cond (set? v)
                     (assoc output k (set (map #(key-merge [kns %]) v)))

                     :else
                     (assoc output k (key-merge [kns v]))))

           :else
           (assoc output k v)))))

(defn- process-init-ref [meta v opts]
  (let [nsvec (key-unmerge (:ref-ns meta))
        data  (extend-keys v nsvec #{:+ :#})]
    (process-init data (:geni opts) opts)))

(defn process-defaults-merge
  [idata fgeni mergeks]
  (if-let [k (first mergeks)]
    (let [dft (-> (fgeni k) first :default)
          dft (if (fn? dft) (dft) dft)]
      (process-defaults-merge
       (assoc idata k dft)
       fgeni
       (next mergeks)))
    idata))

(defn process-defaults-ref
  [idata fgeni assocks opts]
  (if-let [k (first assocks)]
    (let [ref-ns (-> (k fgeni) first :ref-ns)]
      (assoc idata k (process-defaults (idata k) opts ref-ns)))
    idata))

(defn process-defaults [idata opts & [nss-ex]]
  (if (:defaults? opts)
    (let [nss (list-key-ns idata)
          nss (if (nil? nss-ex) nss (conj nss nss-ex))
          fgeni (:fgeni opts)
          ks (as/find-default-keys fgeni nss)
          refks (as/find-ref-keys fgeni nss)
          dataks  (set (keys idata))
          mergeks  (clojure.set/difference ks dataks)
          assocks  (clojure.set/intersection refks dataks)]
      (-> idata
          (process-defaults-merge fgeni mergeks)
          (process-defaults-ref   fgeni assocks opts)))
    idata))


(defn process-required-merge
  [idata fgeni mergeks]
  ;;(println mergeks)
  (if (empty? mergeks) idata
    (throw (Exception. (str "The following keys are required: " mergeks)))))

(defn process-required-ref
  [idata fgeni assocks opts]
  (if-let [k (first assocks)]
    (let [ref-ns (-> (k fgeni) first :ref-ns)]
      (assoc idata k (process-required (idata k) opts ref-ns)))
    idata))

(defn process-required [idata opts & [nss-ex]]
  ;;(println (:required? opts))
  (if (:required? opts)
    (let [nss (list-key-ns idata)
          nss (if (nil? nss-ex) nss (conj nss nss-ex))
          fgeni (:fgeni opts)
          ks (as/find-required-keys fgeni nss)
          dataks  (set (keys idata))
          mergeks  (clojure.set/difference ks dataks)
          refks (as/find-ref-keys fgeni nss)
          assocks  (clojure.set/intersection refks dataks)]
      (-> idata
          (process-required-merge fgeni mergeks)
          (process-required-ref   fgeni assocks opts)))
    idata))
