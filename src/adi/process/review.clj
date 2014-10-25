(ns adi.process.review
  (:require [hara.common.checks :refer [hash-map? long?]]
            [hara.common.error :refer [error]]
            [hara.string.path :as path]
            [clojure.set :as set]))

(defn find-keys
  ([fschm mk mv]
     (find-keys fschm (constantly true) mk mv))
  ([fschm nss mk mv]
     (let [comp-fn  (fn [val cmp] (if (fn? cmp) (cmp val) (= val cmp)))
           filt-fn  (fn [k] (and (nss (path/path-ns k))
                                (comp-fn (mk (first (fschm k))) mv)))]
       (set (filter filt-fn (keys fschm))))))

(defn review-required
  [pdata fsch ks env]
  (if (empty? ks) pdata
      (error "The following keys are required: " ks)))

(defn review-defaults
  [pdata fsch ks env]
  (if-let [k (first ks)]
    (let [[meta] (fsch k)
          t      (:type meta)
          dft    (:default meta)
          value  (if (fn? dft) (dft) dft)
          value  (if (and (not (set? value))
                          (or (= "query" (:type env))
                              (= :many (:cardinality meta))))
                   #{value} value)
          npdata  (cond (or (= t :keyword) (= t :enum))
                       (assoc pdata k value)
                       :else
                       (assoc pdata k value))]
      (recur npdata fsch (next ks) env))
    pdata))

(defn expand-ns-keys
 ([k] (expand-ns-keys k #{}))
 ([k output]
    (if (nil? k) output
      (if-let [nsk (path/path-ns k)]
        (expand-ns-keys nsk (conj output k))
        (conj output k)))))

(defn expand-ns-set
 ([s] (expand-ns-set s #{}))
 ([s output]
    (if-let [k (first s)]
      (expand-ns-set (next s)
                     (set/union output
                                (expand-ns-keys k)))
      output)))

(declare review-current)

(defn review-fn [pdata fsch merge-fn env]
 (let [nss   (expand-ns-set (get-in pdata [:# :nss]))
       ks    (find-keys fsch nss (-> merge-fn :label) (complement nil?))
       refks (find-keys fsch nss :type :ref)
       dataks     (set (keys pdata))
       mergeks    (set/difference ks dataks)
       datarefks  (set/intersection refks dataks)]
   (println nss ks refks)
   (-> pdata
       ((-> merge-fn :function) fsch mergeks env)
       (review-current fsch datarefks merge-fn env))))

(defn review-current
 [pdata fsch ks merge-fn env]
 (if-let [k (first ks)]
   (let [meta   (-> (fsch k) first)
         pr-fn  (fn [rf] (review-fn rf fsch merge-fn env))
         npdata  (if (or (= "query" (:type env))
                         (= :many (:cardinality meta)))
                  (assoc pdata k (set (map pr-fn (pdata k))))
                  (assoc pdata k (pr-fn (pdata k))))]
     (recur npdata fsch (next ks) merge-fn env))
   pdata))

(defn review [pdata env]
  (if (and (not= "query" (:type env))
           (or (-> env :options :schema-defaults)
               (-> env :options :schema-required)))
    (let [fsch   (-> env :schema :flat)
          pdata  (if (-> env :options :schema-defaults)
                   (review-fn pdata fsch
                              {:label :default
                               :function review-defaults}
                              env)
                   pdata)
          pdata  (if (-> env :options :schema-required)
                   (review-fn pdata fsch
                              {:label :required
                               :function review-required}
                              env)
                   pdata)]
      pdata)
    pdata))
