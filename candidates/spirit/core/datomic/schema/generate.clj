(ns spirit.core.datomic.schema.generate
  (:require [spirit.core.datomic.schema.base :as base]
            [hara.data.path :as data]
            [hara.string.path :as path]
            [hara.common.error :refer [error]]))

(defn datomic-attr-property
  {:added "0.3"}
  [attr k pnuema res]
  (let [dft  (:default pnuema)
        v    (or (k attr) dft)
        prop-pair  (fn [attr k v f]
                     [(keyword (str "db/" (name attr)))
                      (f v k)])]
    (cond (nil? v)
          (if (:required pnuema)
            (error "DATOMIC-ATTR-PROPERTY: Property " k " is required")
            res)

          :else
          (let [chk  (or (:check pnuema) (constantly true))
                f    (or (:fn pnuema) (fn [x & xs] x))
                attr (or (:attr pnuema) k)]
            (if (not (chk v))
              (error  "DATOMIC-ATTR-PROPERTY: Property " v
                      " failed check on " attr " for check " chk)
              (apply assoc res (prop-pair attr k v f)))))))

(defn datomic-attr
  [[attr]]
  (reduce-kv (fn [out k v]
               (datomic-attr-property attr k v out))
             {:db.install/_attribute :db.part/db}
             base/datomic-specific))

(defn datomic-enum
  [[attr]]
  (map (fn [v]
         {:db/ident (path/join [(-> attr :enum :ns) v])})
       (-> attr :enum :values)))

(defn datomic-schema
  [essence]
  (let [attrs (-> essence
                  data/flatten-keys-nested)
        [enums attrs] (reduce-kv (fn [[enums rest] _ [{:keys [type]} :as attr]]
                                (if (= type :enum)
                                 [(conj enums attr) rest]
                                 [enums (conj rest attr)]))
                              [[] []]
                              attrs)
        attrs (mapv datomic-attr attrs)
        enum-attrs (->> enums
                        (map #(assoc-in % [0 :type] :ref))
                        (map datomic-attr))
        enum-data  (mapcat datomic-enum enums)]
    (concat attrs enum-attrs enum-data)))
