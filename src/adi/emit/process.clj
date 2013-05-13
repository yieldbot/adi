(ns adi.emit.process
  (:use adi.utils
        hara.hash-map
        hara.common
        [hara.control :only [if-let]]
        [adi.emit.adjust :only [adjust]])
  (:require [adi.schema :as as])
  (:refer-clojure :exclude [if-let]))

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
     (let [tdata (-> (treeify-keys-nested data) ;;; **** THIS IS THE PROBLEM
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
      (flatten-keys-nested-keep)
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
       (-> (process-init {} (treeify-keys-nested data) geni env)
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
        data  (nest-keys rf nsvec #{:+ :#})]
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
