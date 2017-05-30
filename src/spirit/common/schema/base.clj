(ns spirit.common.schema.base
  (:require [hara.common.checks :refer [boolean?]]))

(def base-meta
  {:ident        {:required true
                  :check keyword?}
   :type         {:required true
                  :default :string
                  :auto true}
   :cardinality  {:check #{:one :many}
                  :auto true
                  :default :one}
   :doc          {:check string?}
   :unique       {:check #{:value :identity}}
   :index        {:check boolean?}
   :required     {:check boolean?}
   :restrict     {:check ifn?}
   :default      {:check identity}})

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

(defn defaults [[k prop]]
  (-> (select-keys prop [:default :auto])
      (assoc :id k)))

(defn all-auto-defaults
  ([] (all-auto-defaults base-meta))
  ([meta]
   (filter (fn [m] (-> m :auto))
           (map defaults meta))))

(defn all-defaults
  ([] (all-defaults base-meta))
  ([meta]
   (filter (fn [m] (-> m :default nil? not))
           (map defaults meta))))
