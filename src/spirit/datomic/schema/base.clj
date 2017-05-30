(ns spirit.datomic.schema.base
  (:require [spirit.common :as common]
            [hara.common.checks :refer [boolean? type-checker]]
            [hara.data.nested :as nested]))

(defn schema-property
  [val ns]
  (keyword (str "db." (name ns) "/" (name val))))

(def datomic-additions
  {:ident        {}
   :type         {:check #{:keyword :string :boolean :long :bigint :float :enum
                           :double :bigdec :ref :instant :uuid :uri :bytes}
                  :attr :valueType
                  :fn schema-property}
   :cardinality  {:fn schema-property}
   :doc          {}
   :unique       {:fn schema-property}
   :index        {:default false}
   :fulltext     {:default false}
   :isComponent  {:check boolean?}
   :noHistory    {:check boolean?
                  :default false}})

(def datomic-meta
  (->> datomic-additions
       (reduce-kv (fn [out k v]
                    (assoc out k (assoc v :schema true)))
                  {})
       (nested/merge-nested common/base)))

(def datomic-specific
  (->> datomic-meta
       (reduce-kv (fn [out k v]
                    (if (:schema v)
                      (assoc out k v)
                      out))
                  {})))

(def type-checks
  (let [types (-> datomic-meta :type :check)]
    (zipmap types (map type-checker types))))

(defn defaults [[k prop]]
  (-> (select-keys prop [:default :auto])
      (assoc :id k)))

(def all-defaults
  (filter (fn [m] (-> m :default nil? not))
          (map defaults datomic-meta)))

(def all-auto-defaults
  (filter (fn [m] (-> m :auto))
          (map defaults datomic-meta)))


