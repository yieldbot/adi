(ns adi.schema.find
  (:require [hara.common :refer [error hash-map? keyword-ns keyword-split]]))

(defn find-keys
  ([fschm mk mv]
     (find-keys fschm (constantly true) mk mv))
  ([fschm nss mk mv]
     (let [comp-fn  (fn [val cmp] (if (fn? cmp) (cmp val) (= val cmp)))
           filt-fn  (fn [k] (and (nss (keyword-ns k))
                                (comp-fn (mk (first (fschm k))) mv)))]
       (set (filter filt-fn (keys fschm)))))
  ([fschm nss mk mv mk1 mv1 & more]
     (let [ks (find-keys fschm nss mk mv)
           nfschm (select-keys fschm ks)]
       (apply find-keys nfschm nss mk1 mv1 more))))

(defn find-required-keys
  ([fschm] (find-keys fschm :required true))
  ([fschm nss] (find-keys fschm nss :required true)))

(defn find-default-keys
  ([fschm] (find-keys fschm :default true?))
  ([fschm nss] (find-keys fschm nss :default true?)))

(defn find-ref-keys
  ([fschm] (find-keys fschm :type :ref))
  ([fschm nss] (find-keys fschm nss :type :ref)))

;; UNTESTED
(defn schema-nss [schema nss]
  (->> (keyword-split nss)
       (get-in schema)))

(defn schema-required-keys
  ([res]
     (cond (vector? res)
           [((fn [[prop]]
               (if (:required prop) (:ident prop)))
             res)]

           (hash-map? res)
           (mapcat #(schema-required-keys %) (vals res))

           :else
           (error res " is not a valid namespace")))
  ([schema nss]
     (-> (schema-required-keys (schema-nss schema nss))
         (set)
         (disj nil))))

(defn schema-property? [schema nss]
  (vector? (schema-nss schema nss)))
