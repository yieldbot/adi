(ns adi.process.pack.review
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
  [pdata fsch ks adi]
  (if (empty? ks) pdata
      (error "The following keys are required: " ks)))

(defn review-defaults
  [pdata fsch ks adi]
  (if-let [k (first ks)]
    (let [[attr] (fsch k)
          t      (:type attr)
          dft    (:default attr)
          value  (if (fn? dft) (dft) dft)
          value  (if (and (not (set? value))
                          (or (= "query" (:type adi))
                              (= :many (:cardinality attr))))
                   #{value} value)
          npdata  (cond (= t :keyword)
                        (assoc pdata k value)

                        (= t :enum)
                        (assoc pdata k (if-let [ens (-> attr :enum :ns)]
                                         (path/join [ens value])
                                         value))
                        :else
                        (assoc pdata k value))]
      (recur npdata fsch (next ks) adi))
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

(defn wrap-id [f k]
  (fn [pdata fsch merge-fn adi]
    (if (and (long? pdata)
             (-> adi :schema :flat k first :type (= :ref)))
      pdata
      (f pdata fsch merge-fn adi))))

(defn review-fn [pdata fsch merge-fn adi]
 (let [nss   (expand-ns-set (get-in pdata [:# :nss]))
       ks    (find-keys fsch nss (-> merge-fn :label) (complement nil?))
       refks (find-keys fsch nss :type :ref)
       dataks     (try (set (keys pdata))
                       (catch Throwable t
                         (throw t)))
       mergeks    (set/difference ks dataks)
       datarefks  (set/intersection refks dataks)]
   (-> pdata
       ((-> merge-fn :function) fsch mergeks adi)
       (review-current fsch datarefks merge-fn adi))))

(defn review-current
  [pdata fsch ks merge-fn adi]
  (if-let [k (first ks)]
    (let [meta   (-> (fsch k) first)
          pr-fn  (fn [rf] ((wrap-id review-fn k) rf fsch merge-fn adi))
          npdata  (if (or (= "query" (:type adi))
                          (= :many (:cardinality meta)))
                    (assoc pdata k (set (map pr-fn (pdata k))))
                    (assoc pdata k (pr-fn (pdata k))))]
      (recur npdata fsch (next ks) merge-fn adi))
    pdata))

(defn review-raw
  "
  (review {:# {:nss #{:account} :account/name \"Chris\"}}
          {:schema (schema/schema {:account {:name [{:required true}]
                                             :age  [{:required true}]}})
           :options {:schema-required true}})
  => (throws)"
  {:added "0.3"}
  [pdata adi]
  (if (and (not= "query" (:type adi))
           (or (-> adi :options :schema-defaults)
               (-> adi :options :schema-required)))
    (let [fsch   (-> adi :schema :flat)
          pdata  (if (-> adi :options :schema-defaults)
                   (review-fn pdata fsch
                              {:label :default
                               :function review-defaults}
                              adi)
                   pdata)
          pdata  (if (-> adi :options :schema-required)
                   (review-fn pdata fsch
                              {:label :required
                               :function review-required}
                              adi)
                   pdata)]
      pdata)
    pdata))

(defn review [adi]
  (let [data (-> adi :process :analysed)
        ndata (review-raw data adi)]
    (assoc-in adi [:process :reviewed] ndata)))
