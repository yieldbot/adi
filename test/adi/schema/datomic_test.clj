(ns adi.schema.datomic-test
  (:use midje.sweet)
  (:require [adi.schema.datomic :refer :all]
            [adi.schema.ref :as ref]
            [adi.schema.meta :as meta]))

^{:refer adi.schema.datomic/datomic-attr-property :added "0.3"}
(fact "creates a property description from an adi one"
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

^{:refer adi.schema.datomic/datomic-attr :added "0.3"}
(fact "creates a field description from a single adi attribute"
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

^{:refer adi.schema.datomic/datomic-enum :added "0.3"}
(fact "creates schema idents from the adi enum attr"
  (->> (datomic-enum [{:ident   :person/gender
                       :type    :enum
                       :enum    {:ns     :person.gender
                                 :values #{:male  :female}}}])
       (map #(dissoc % :db/id)))
  => [{:db/ident :person.gender/female}
      {:db/ident :person.gender/male}])

^{:refer adi.schema.datomic/datomic :added "0.3"}
(fact "creates a datomic-compatible schema from an adi one"
  (->> (datomic {:node/male   [{:ident :node/male
                                :type  :ref
                                :ref {:ns  :node}}]
                 :person/gender [{:ident   :person/gender
                                  :type    :enum
                                  :enum    {:ns     :person.gender
                                            :values #{:male  :female}}}]})
       (map #(dissoc % :db/id)))
  => [{:db.install/_attribute :db.part/db,
       :db/cardinality :db.cardinality/one,
       :db/ident :node/male,
       :db/valueType :db.type/ref}
      {:db/ident :person.gender/female}
      {:db/ident :person.gender/male}])
