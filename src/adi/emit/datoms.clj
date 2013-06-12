(ns adi.emit.datoms
  (:use hara.common
        [hara.control :only [if-let]]
        [adi.emit.process :only [process]]
        [adi.emit.characterise :only [characterise]])
  (:require [adi.schema :as as])
  (:refer-clojure :exclude [if-let]))

(declare datoms
         datoms-data-one datoms-data-many
         datoms-refs-one datoms-refs-many)

(defn datoms
  "Outputs a datomic structure from characterised result"
  ([chdata]
    (concat (datoms chdata datoms-data-one datoms-data-many)
            (datoms chdata datoms-refs-one datoms-refs-many)))
  ([chdata f1 f2]
    (cond (nil? (seq chdata)) []
        :else
        (concat (mapcat (fn [x] (datoms (second x) f1 f2))
                        (:refs-one chdata))
                (mapcat (fn [x]
                          (mapcat #(datoms % f1 f2) (second x)))
                        (:refs-many chdata))
                (f1 chdata)
                (f2 chdata)))))

;; Datoms Helper Functions

(defn datoms-data-one [chd]
  [(assoc (:data-one chd) :db/id (get-in chd [:db :id]))])

(defn datoms-data-many [chd]
  (for [[k vs] (:data-many chd)
        v vs]
    [:db/add (get-in chd [:db :id]) k v]))

(defn datoms-refs-one [chd]
  (for [[k ref] (:refs-one chd)]
    (if (as/ident-reversed? k)
      [:db/add (get-in ref [:db :id]) (as/flip-ident k) (get-in chd [:db :id])]
      [:db/add (get-in chd [:db :id]) k (get-in ref [:db :id])])))

(defn datoms-refs-many [chd]
  (for [[k refs] (:refs-many chd)
        ref refs]
    (if (as/ident-reversed? k)
      [:db/add (get-in ref [:db :id]) (as/flip-ident k) (get-in chd [:db :id])]
      [:db/add (get-in chd [:db :id]) k (get-in ref [:db :id])])))

(defn emit-remove-empty-refs [coll]
  (filter (fn [x]
              (or (vector? x)
                  (and (hash-map? x)
                       (-> (dissoc x :db/id) empty? not))))
          coll))

(defn emit-datoms [data env default-env]
  (cond (or (vector? data) (list? data) (lazy-seq? data))
        (mapcat #(emit-datoms % env default-env) data)

        (hash-map? data)
        (let [menv (merge-nested env default-env)
              chdata  (-> data
                          (process menv)
                          (characterise menv))]
          (emit-remove-empty-refs (datoms chdata)))))

(defn emit-datoms-insert [data env]
  (emit-datoms data env {:generate {:ids {:current true}}}))

(defn emit-datoms-update [data env]
  (emit-datoms data env
               {:generate {:ids {:current true}}
                :options {:required? false
                                   :extras? true
                                   :defaults? false}}))
