(ns adi.schema.meta
  (:require [hara.common :refer [boolean? long? uuid? uri? bigdec? instant?
                                 bytes? type-checker keyword-str]]
            [hara.collection.hash-map :refer [treeify-keys-nested]]
            [adi.schema.types :refer [ref? enum?]]))

(defn meta-schema-property
    "Returns the keyword enumeration for datomic schemas properties.

      (meta-schema-property :string :type)
      ;=> :db.type/string
  "
    [val ns]
    (keyword (str "db." (keyword-str ns) "/" (keyword-str val))))


(def meta-schema
    {:ident        {:required true
                    :check keyword?}
     :type         {:required true
                    :check #{:keyword :string :boolean :long :bigint :float :enum
                             :double :bigdec :ref :instant :uuid :uri :bytes}
                    :default :string
                    :auto true
                    :attr :valueType
                    :fn meta-schema-property}
     :cardinality  {:check #{:one :many}
                    :default :one
                    :auto    true
                    :fn meta-schema-property}
     :unique       {:check #{:value :identity}
                    :fn meta-schema-property}
     :doc          {:check string?}
     :index        {:check boolean?
                    :default false}
     :fulltext     {:check boolean?
                    :default false}
     :isComponent  {:check keyword?}
     :noHistory    {:check boolean?
                    :default false}})

(def meta-type-checks
  (let [types (-> meta-schema :type :check)]
    (zipmap types (map type-checker types))))

(defn mschm-defaults [[k prop]]
  (-> (select-keys prop [:default :auto])
      (assoc :id k)))

(def mschm-all-defaults
  (filter (fn [m] (-> m :default nil? not))
          (map mschm-defaults meta-schema)))

(def mschm-all-auto-defaults
  (filter (fn [m] (-> m :auto))
          (map mschm-defaults meta-schema)))

(defn mschm-attr-add-ident [[k [attr :as v]]]
  [k (assoc-in v [0 :ident] k)])

(defn mschm-attr-add-defaults [[k [attr :as v]] dfts]
  (let [mks   (map :id dfts)
        mdfts (map :default dfts)
        v-defaults (->> (merge (zipmap mks mdfts) attr)
                        (assoc v 0))]
    [k v-defaults]))
