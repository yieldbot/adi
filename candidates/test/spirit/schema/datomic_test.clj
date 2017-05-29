(ns spirit.schema.datomic-test
  (:use hara.test)
  (:require [spirit.schema.datomic :refer :all]
            [spirit.schema.ref :as ref]
            [spirit.schema.meta :as meta]))

^{:refer spirit.schema.datomic/datomic-attr-property :added "0.3"}
(fact "creates a property description from an spirit one"
  (datomic-attr-property {:type :string} :type
                         (meta/meta-schema :type) {})
 => {:db/valueType :db.type/string}

 (datomic-attr-property {:cardinality :one} :cardinality
                        (meta/meta-schema :cardinality) {})
 => {:db/cardinality :db.cardinality/one}

 (datomic-attr-property {} :cardinality
                        (meta/meta-schema :cardinality) {})
 => {:db/cardinality :db.cardinality/one}

 (datomic-attr-property {} :unique
                        (meta/meta-schema :unique) {})
 => {}
 ^:hidden
 (datomic-attr-property {} :type
                        (meta/meta-schema :type) {})
 => {:db/valueType :db.type/string}

 (datomic-attr-property {:type :ERROR} :type
                        (meta/meta-schema :type) {})
 => (throws Exception))

^{:refer spirit.schema.datomic/datomic-attr :added "0.3"}
(fact "creates a field description from a single spirit attribute"
  (datomic-attr [{:ident :name
                  :type  :string}])
  => (contains {:db.install/_attribute :db.part/db,
                :db/ident :name,
                :db/valueType :db.type/string,
                :db/cardinality :db.cardinality/one})

  (datomic-attr [{:ident       :account/tags
                  :type        :string
                  :cardinality :many
                  :fulltext    true
                  :index       true
                  :doc         "tags for account"}])
  => (contains {:db.install/_attribute :db.part/db
                :db/ident        :account/tags
                :db/index        true
                :db/doc          "tags for account"
                :db/valueType    :db.type/string
                :db/fulltext     true
                :db/cardinality  :db.cardinality/many}))

^{:refer spirit.schema.datomic/datomic-enum :added "0.3"}
(fact "creates schema idents from the spirit enum attr"
  (->> (datomic-enum [{:ident   :person/gender
                       :type    :enum
                       :enum    {:ns     :person.gender
                                 :values #{:male  :female}}}])
       (map #(dissoc % :db/id)))
  => [{:db/ident :person.gender/female}
      {:db/ident :person.gender/male}])

^{:refer spirit.schema.datomic/datomic :added "0.3"}
(fact "creates a datomic-compatible schema from an spirit one"
  (->> (datomic {:node/male   [{:ident :node/male
                                :type  :ref
                                :ref {:ns  :node}}]
                 :person/gender [{:ident   :person/gender
                                  :type    :enum
                                  :enum    {:ns     :person.gender
                                            :values #{:male  :female}}}]})
       (map #(dissoc % :db/id)))
  => [{:db.install/_attribute :db.part/db
       :db/cardinality :db.cardinality/one
       :db/ident :node/male
       :db/valueType :db.type/ref}
      {:db.install/_attribute :db.part/db
       :db/cardinality :db.cardinality/one
       :db/ident :person/gender
       :db/valueType :db.type/ref}
      {:db/ident :person.gender/female}
      {:db/ident :person.gender/male}])
