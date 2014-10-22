(ns adi.schema.ref
  (:use [hara.common :only [if-let starts-with? check-all-> error func-map
                            keyword-val keyword-join keyword-ns keyword-root keyword-stemvec]])
  (:require [inflections.core :as inf])
  (:refer-clojure :exclude [if-let]))

;; Functions for Reverse

(defn keyword-reverse [k]
  (let [kval   (name (keyword-val k))
        rkval  (if (starts-with? kval "_")
                 (.substring kval 1)
                 (str "_" kval))]
    (keyword-join [(keyword-ns k) rkval])))

(defn keyword-reversed? [k]
  (-> k keyword-val str (starts-with? ":_")))

(defn is-reversible-attr? [[attr]]
  (check-all->
   attr
   [:type '(= :ref)
    :ref :ns
    [:ref :norev] not
    [:ref :mutual] not
    :ident keyword-ns
    '(:ident keyword-reversed?) not]))

(defn find-ref-attrs [fschm]
  (->> fschm
       (filter (fn [[k [attr]]] (= :ref (:type attr))))
       (into {})))

(defn find-ref-idents [fschm]
  (keys (find-ref-attrs fschm)))

(defn find-reversible-attrs [fschm]
  (->> fschm
       (filter (fn [[k [attr]]] (is-reversible-attr? [attr])))
       (into {})))

(defn find-reversible-idents [fschm]
  (keys (find-reversible-attrs fschm)))

(defn determine-ref-rval [[[root ref-ns many?] [attr] :as entry]]
  (if-let [rval (-> attr :ref :rval)]
    rval
    (let [ident  (:ident attr)
          ival    (keyword-val ident)]

      (cond (= root ref-ns)
            (let [rvec (concat (keyword-stemvec ident) '("of"))]
              (keyword-join rvec "_"))

            many?
            (let [rvec (concat (keyword-stemvec ident)
                               (list (->> root name inf/plural)))]
              (keyword-join rvec "_"))

            :else
            (->> root name inf/plural keyword)))))

(defn prepare-forward-attr [[attr]]
  (if-let [ident  (:ident attr)
           f-ref  (:ref attr)
           f-ns   (:ns f-ref)
           f-rval (:rval f-ref)]
    (let [n-ref {:type    :forward
                 :key     ident
                 :val     (keyword-val ident)
                 :rkey    (keyword-reverse ident)
                 :rident  (keyword-join [f-ns f-rval])}]
      [(assoc attr :ref (merge f-ref n-ref))])
    (error "PREPARE_FORWARD_ATTR: Required keys: [ident, ref [ns rval]] " attr)))

(defn prepare-reverse-attr [[attr]]
  (if-let [ident   (:ident attr)
           f-ref   (:ref   attr)
           f-key   (:key   f-ref)
           f-val   (:val   f-ref)
           f-rkey  (:rkey  f-ref)
           f-rval  (:rval  f-ref)
           f-rident (:rident f-ref)]
    [{:ident       f-rident
      :cardinality :many
      :type        :ref
      :ref         {:ns      (keyword-root ident)
                    :type    :reverse
                    :val     f-rval
                     :key     f-rkey
                     :rval    f-val
                     :rkey    f-key
                     :rident  ident}}]
    (error "PREPARE_REVERSE_ATTR: Required keys: [ident, ref [key val rkey rval rident]" attr)))

(defn prepare-ref-attr
  [[_ [attr] :as entry]]
  (prepare-forward-attr
   [(assoc-in attr [:ref :rval] (determine-ref-rval entry))]))


(defn mark-reversible-attrs
    ([nsgroups] (mark-reversible-attrs nsgroups []))
    ([[nsgroup & more] output]
       (if-let [[nspair entries] nsgroup]
         (cond (< 1 (count entries))
               (recur more
                      (concat output
                              (map (fn [m] [(conj nspair true) m]) entries)))
               :else
               (recur more
                      (conj output [(conj nspair false) (first entries)])))
         output)))

(defn attr-ref-ns-pair [[attr]]
  (let [ident  (:ident attr)
        ref-ns (->  attr :ref :ns)]
    [(keyword-root ident) ref-ns]))

(defn prepare-all-ref-attrs [fschm]
  (let [rfs   (vals (find-reversible-attrs fschm))
        lus   (group-by attr-ref-ns-pair rfs)]
    (->> (mark-reversible-attrs (seq lus))
         (map prepare-ref-attr))))

(defn prepare-all-rev-attrs [refs]
  (->> refs
       (filter is-reversible-attr?)
       (map prepare-reverse-attr)))
