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
           chk (:restrict meta)]
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
         process-list-nss process-unnest-key process-keyword-assoc
         process-init process-init-assoc process-init-ref
         process-defaults process-defaults-merge process-defaults-ref
         process-required process-required-merge process-required-ref)

(defn process-unnest-key
  ([data] (process-unnest-key data :+))
  ([data k]
     (if-let [ndata (data k)]
       (merge (dissoc data k)
              (process-unnest-key ndata k))
       data)))

(defn process-list-nss [data]
  (let [rm-ex (fn [s] (disj s :# :db))]
    (-> (treeify-keys-in data)
        (process-unnest-key :+)
        keys
        set
        rm-ex)))

(defn process-keyword-assoc [output meta k v]
  (let [kns (:keyword-ns meta)]
    (cond (set? v)
          (assoc output k (set (map #(keyword-join [kns %]) v)))

          :else
          (assoc output k (keyword-join [kns v])))))

(defn process-init
  ([data geni env]
     (let [nss (process-list-nss data)]
       (-> (process-init {} (treeify-keys-in data) geni
                         (assoc env :nss nss))
           (assoc-in [:# :nss] nss))))
  ([output data geni env]
     (if-let [[k v] (first data)]
       (cond (= k :+) ;; if :+, reset the geni
             (merge (process-init {} v (-> env :schema :geni) env)
                    (process-init output (next data) geni env))

             (or (= k :#) (= k :db)) ;; add and continue
             (assoc (process-init output (next data) geni env) k v)

             (not (contains? geni k))
             (if (-> env :options :extras?)
               (process-init output (next data) geni env)
               (throw (Exception.
                       (format "Data not found in schema definition (%s %s)\n  %s  \n"
                               k v geni))))

             (vector? (geni k))
             (-> (process-init-assoc output (geni k) v env)
                 (process-init (next data) geni env))

             (hash-map? (geni k))
             (merge (process-init {} v (geni k) env)
                    (process-init output (next data) geni env)))
       output)))


(defn- process-init-ref [meta rf env]
  (let [nsvec (keyword-split (:ref-ns meta))
        data  (nest-keys-in rf nsvec #{:+ :#})]
    (process-init data (-> env :schema :geni)
                  (assoc env :nss (process-list-nss rf)))))

(defn process-init-assoc [output [meta] v env]
  (cond
   (nil? v) output

   :else
   (let [id (:ident meta)
         t (:type meta)
         v (adjust v meta env)]
     (cond (= t :ref)
           (let [rk (:ref-key meta)
                 k  (or rk id)
                 output (if rk (assoc-in output [:# :aliases id] rk)
                            output)]
             (if (set? v)
               (assoc output k (set (map #(process-init-ref meta % env) v)))
               (assoc output k (process-init-ref meta v env))))

           (and (= t :keyword) (:keyword-ns meta))
           (process-keyword-assoc output meta id v)

           :else
           (assoc output id v)))))

(defn process-init-env [geni env]
  (let [schema  (or (:schema env) (as/make-scheme-model geni))
        opts    (or (:options env) {})
        mopts   {:defaults? (if (nil? (:defaults? opts)) true (:defaults? opts))
                 :restrict? (if (nil? (:restrict? opts)) true (:restrict? opts))
                 :required? (if (nil? (:required? opts)) true (:required? opts))
                 :extras? (or (:extras? opts) false)
                 :sets-only? (or (:sets-only? opts) false)}]
    (assoc env :schema schema :options mopts)))

(comment

  (defn process
    ([data geni] (process data geni {}))
    ([data geni env]
       (let [menv (process-init-env geni env)]
         (-> (process-init data geni menv)
             ;;(process-defaults menv)
             ;;(process-required menv)
             )))))
