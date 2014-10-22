(ns adi.schema.emit
  (:require [hara.common :refer [if-let error func-map keyword-str keyword-join]]
            [adi.schema.meta :refer [meta-schema]]
            [datomic.api :refer [tempid]])
  (:refer-clojure :exclude [if-let]))

(defn make-enum-rel [[attr]]
  (-> attr
      (assoc :type :ref :ref (:enum attr))
      (dissoc :enum)
      (assoc-in [:ref :type] :enum-rel)
      vector))

(defn find-revref-attrs [fschm]
  (->> fschm
       (filter (fn [[k [attr]]]
                 (and (= :ref (:type attr))
                      (= :reverse (-> attr :ref :type)))))
       (into {})))

(defn find-revref-idents [fschm]
  (keys (find-revref-attrs fschm)))

(defn remove-revref-attrs [fschm]
  (let [rvs   (find-revref-idents fschm)]
    (apply dissoc fschm rvs)))


(defn find-attrs [fschm t]
  (->> fschm
       (filter (fn [[k [meta]]] (= t (:type meta))))
       (into {})))

(defn find-idents [fschm t]
  (keys (find-attrs fschm t)))

(defn remove-attrs [fschm t]
  (let [es     (find-attrs fschm t)
        enums  (vals es)
        e-rels (map make-enum-rel enums)
        mfschm (apply dissoc fschm (keys es))
        get-ident (fn [[meta]] (:ident meta))]
    (merge mfschm
           (func-map get-ident e-rels))))

(defn remove-enum-attrs [fschm]
  (remove-attrs fschm :enum))

(defn remove-alias-attrs [fschm]
  (remove-attrs fschm :alias))

(defn emit-dschm-attr-property [attr k mgprop res]
  (let [dft  (if-let [dft  (:default mgprop)
                      add? (:auto mgprop)]
               dft)
        v    (or (k attr) dft)
        prop-pair  (fn [attr k v f]
                     [(keyword (str "db/" (keyword-str attr)))
                      (f v k)])]
    (cond (nil? v)
          (if (:required mgprop)
            (error "EMIT-DSCHEM-ATTR-PROPERTY: Property " k " is required")
            res)

          :else
          (let [chk  (or (:check mgprop) (constantly true))
                f    (or (:fn mgprop) (fn [x & xs] x))
                attr (or (:attr mgprop) k)]
            (if (not (chk v))
              (error  "EMIT-DSCHEM-ATTR-PROPERTY: Property " v
                      " failed check on " attr " for check " chk)
              (apply assoc res (prop-pair attr k v f))
              )))))

(defn emit-dschm-attr
  ([[attr]] (emit-dschm-attr [attr] meta-schema {}))
  ([[attr] mg output]
     (if-let [[k v] (first mg)]
       (recur [attr]
              (rest mg)
              (emit-dschm-attr-property attr k v output))
       (assoc output
         :db.install/_attribute :db.part/db
         :db/id (tempid :db.part/db)))))

(defn emit-dschm-enum
  ([[attr]]
     (map (fn [v]
            {:db/id (tempid :db.part/user)
             :db/ident (keyword-join [(-> attr :enum :ns) v])})
          (-> attr :enum :values))))

(defn emit-dschm
  ([fschm]
     (let [enums  (vals (find-attrs fschm :enum))
           attrs  (-> fschm remove-revref-attrs
                            remove-enum-attrs
                            remove-alias-attrs
                            vals)]
       (concat
        (mapv emit-dschm-attr attrs)
        (mapcat emit-dschm-enum enums))))
  ([fschm & more] (emit-dschm (apply merge fschm more))))
