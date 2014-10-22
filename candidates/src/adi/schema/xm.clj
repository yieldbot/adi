(ns adi.schema.xm
  (:require [hara.common :refer [func-map]]
            [hara.collection.hash-map :refer [treeify-keys treeify-keys-nested
                                              flatten-keys-nested]]
            [adi.schema.ref :refer [prepare-all-ref-attrs prepare-all-rev-attrs]]
            [adi.schema.meta :refer [mschm-attr-add-ident
                                     mschm-all-auto-defaults
                                     mschm-attr-add-defaults]]))

(defn prepare-aliases [fschm]
  (let [aliases (->> fschm
                     (filter (fn [[k [attr]]]
                               (= :alias (:type attr))))
                     (map second))]
    (map #(assoc-in % [0 :alias :template]
                    (treeify-keys-nested (-> % (get 0) :alias :template)))
         aliases)))

(defn fschm-add-idents
  "Takes a flattened schema and adds the :ident keyword to all attribute definitions"
  [fschm]
  (->> (map mschm-attr-add-ident fschm)
       (into {})))

(defn fschm-add-defaults
  ([fschm] (fschm-add-defaults fschm mschm-all-auto-defaults))
  ([fschm dfts]
     (->> (map #(mschm-attr-add-defaults % dfts) fschm)
          (into {}))))

(defn fschm-add-refs [fschm]
  (let [refs (prepare-all-ref-attrs fschm)
        revs (prepare-all-rev-attrs refs)
        aliases (prepare-aliases fschm)
        get-ident (fn [[attr]] (:ident attr))]
    (merge fschm
           (func-map get-ident refs)
           (func-map get-ident revs)
           (func-map get-ident aliases))))

(defn fschm-prepare-ischm [ischm]
  (-> (flatten-keys-nested ischm)
      fschm-add-idents
      fschm-add-defaults
      fschm-add-refs))

(defn make-xm-lu
  ([fschm] (make-xm-lu fschm {}))
  ([fschm output]
     (if-let [[k [meta]] (first fschm)]
       (cond (and (= :ref (:type meta))
                  (= :reverse (-> meta :ref :type)))
             (recur (next fschm) (assoc output (-> meta :ref :key) k k k))

             :else
             (recur (next fschm) (assoc output k k)))
       output)))

(defn make-xm [ischm]
    (let [fschm (fschm-prepare-ischm ischm)]
    {:tree  (treeify-keys fschm)
     :flat  fschm
     :lu    (make-xm-lu fschm)}))
