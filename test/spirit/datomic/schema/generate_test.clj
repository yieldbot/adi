(ns spirit.datomic.schema.generate-test
  (:use hara.test)
  (:require [spirit.datomic.schema.generate :refer :all]
            [spirit.datomic.schema.base :as base]))

^{:refer spirit.datomic.schema.generate/datomic-attr-property :added "0.1"}
(fact "creates a datomic property description from spirit"
  (datomic-attr-property {:type :string} :type
                         (base/datomic-specific :type) {})
  => {:db/valueType :db.type/string}

  (datomic-attr-property {:cardinality :one} :cardinality
                        (base/datomic-specific :cardinality) {})
  => {:db/cardinality :db.cardinality/one}

  (datomic-attr-property {} :cardinality
                        (base/datomic-specific :cardinality) {})
  => {:db/cardinality :db.cardinality/one}

  (datomic-attr-property {} :unique
                        (base/datomic-specific :unique) {})
  => {})

^{:refer spirit.datomic.schema.generate/datomic-attr :added "0.1"}
(fact "creates a datomic attribute schema entry"
  (datomic-attr [{:ident :account/name
                  :type  :string}])
  => (contains {:db.install/_attribute :db.part/db,
                :db/index false,
                :db/fulltext false,
                :db/noHistory false,
                :db/valueType :db.type/string,
                :db/ident :account/name,
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

^{:refer spirit.datomic.schema.generate/datomic-enum :added "0.1"}
(fact "creates datomic idents from an `:enum` attr"
  (->> (datomic-enum [{:ident   :person/gender
                       :type    :enum
                       :enum    {:ns     :person.gender
                                 :values #{:male  :female}}}])
       (map #(dissoc % :db/id)))
  => [{:db/ident :person.gender/female}
      {:db/ident :person.gender/male}])

^{:refer spirit.datomic.schema.generate/datomic :added "0.1"}
(fact "creates a full datomic schema from spirit"
  (->> (datomic-schema {:node/link   [{:ident :node/link
                                       :type  :ref
                                       :ref {:ns  :node}}]
                        :person/gender [{:ident   :person/gender
                                         :type    :enum
                                         :enum    {:ns     :person.gender
                                                   :values #{:male  :female}}}]})
       (map #(dissoc % :db/id)))
  => [{:db.install/_attribute :db.part/db,
       :db/index false,
       :db/fulltext false,
       :db/noHistory false,
       :db/valueType
       :db.type/ref,
       :db/ident :node/link,
       :db/cardinality :db.cardinality/one}
      {:db.install/_attribute :db.part/db,
       :db/index false,
       :db/fulltext false,
       :db/noHistory false,
       :db/valueType :db.type/ref,
       :db/ident :person/gender,
       :db/cardinality :db.cardinality/one}
      {:db/ident :person.gender/female}
      {:db/ident :person.gender/male}])
