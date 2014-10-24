(ns adi.schema.meta
  (:require [hara.common.checks :refer
             [boolean? long? uuid? uri? bigdec? bigint? instant? bytes? double? type-checker]]
            [adi.data.checks :refer [enum? ref?]]))

(defn schema-property
  "constructs a :db property out of a value and a type
  (meta/schema-property :string :type) => :db.type/string
  (meta/schema-property :long :type) => :db.type/long
  (meta/schema-property :one :cardinality) => :db.cardinality/one
  (meta/schema-property :value :unique) => :db.unique/value"
  {:added "0.3"}
  [val ns]
  (keyword (str "db." (name ns) "/" (name val))))

(def meta-schema
  {:ident        {:required true
                  :check keyword?}
   :type         {:required true
                  :check #{:keyword :string :boolean :long :bigint :float :enum
                           :double :bigdec :ref :instant :uuid :uri :bytes}
                  :default :string
                  :auto true
                  :attr :valueType
                  :fn schema-property}
   :cardinality  {:check #{:one :many}
                  :default :one
                  :auto    true
                  :fn schema-property}
   :unique       {:check #{:value :identity}
                  :fn schema-property}
   :doc          {:check string?}
   :index        {:check boolean?
                  :default false}
   :fulltext     {:check boolean?
                  :default false}
   :isComponent  {:check keyword?}
   :noHistory    {:check boolean?
                  :default false}})

(def type-checks
  (let [types (-> meta-schema :type :check)]
    (zipmap types (map type-checker types))))

(defn defaults [[k prop]]
  (-> (select-keys prop [:default :auto])
      (assoc :id k)))

(def all-defaults
  (filter (fn [m] (-> m :default nil? not))
          (map defaults meta-schema)))

(def all-auto-defaults
  (filter (fn [m] (-> m :auto))
          (map defaults meta-schema)))

(defn attr-add-ident
  "adds the key of a pair as :ident to a schema property pair
  (meta/attr-add-ident [:person [{}]])
  => [:person [{:ident :person}]]

  (meta/attr-add-ident [:person/address [{}]])
  => [:person/address [{:ident :person/address}]]"
  {:added "0.3"}
  [[k [attr :as v]]]
  [k (assoc-in v [0 :ident] k)])

(defn attr-add-defaults
  "adds defaults to a given schema property pair
  (meta/attr-add-defaults [:person [{}]] [])
  => [:person [{}]]

  (meta/attr-add-defaults [:person [{}]]
                          meta/all-auto-defaults)
  => [:person [{:cardinality :one :type :string}]]

  (meta/attr-add-defaults [:person [{:cardinality :many :type :long}]]
                          meta/all-auto-defaults)
  => [:person [{:cardinality :many
                :type :long}]]

  (meta/attr-add-defaults [:person [{}]]
                          meta/all-defaults)
  => [:person [{:index false
                :fulltext false
                :cardinality :one
                :noHistory false
                :type :string}]]"
  {:added "0.3"}
  [[k [attr :as v]] dfts]
  (let [mks   (map :id dfts)
        mdfts (map :default dfts)
        v-defaults (->> (merge (zipmap mks mdfts) attr)
                        (assoc v 0))]
    [k v-defaults]))
