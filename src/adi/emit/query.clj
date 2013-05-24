(ns adi.emit.query
  (:use hara.hash-map
        [hara.common :only [??-fn merge-nested]]
        [adi.utils :only [?gensym walk-replace]]
        [adi.emit.process :only [process]]
        [adi.emit.characterise :only [characterise]])
  (:require [adi.schema :as as]))

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

(def ?init '[??sym ??attr ?])

(defn has-placeholder? [lst]
  (some #(= '? %) lst))

(defn q-fn [[x & xs :as lst]]
  (if (has-placeholder? lst)
    [?init [lst]]
    [?init [(apply list x '? xs)]]))

(defn not-fn [val]
  [?init [(list 'not= '? val)]])

(defn fulltext-fn [val]
  [[(list 'fulltext '$ '??attr val) [['??sym '?]]]])

(defn query-parse-list [[x & xs :as lst]]
  (cond
   (= x '?fulltext) (fulltext-fn (second lst))
   (= x '?not) (not-fn (second lst))
   :else (q-fn lst)))

(defn query-data-val [sym k v env]
  (cond (list? v)
        (query-data-val sym k (query-parse-list v) env)

        (vector? v)
        (let [symgen (or (-> env :generate :syms :function) ?gensym)
              esym (symgen)]
          (walk-replace v {'??sym sym '? esym '??attr k}))
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
  (let [menv (merge-nested env {:options {:restrict? false
                                          :required? false
                                          :defaults? false
                                          :sets-only? true
                                          :query? true}
                            :generate {:syms {:current true}}})
        chdata  (-> data
                    (process menv)
                    (characterise menv))]
    (query chdata menv)))
