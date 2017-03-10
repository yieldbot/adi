(ns adi.schema.meta-test
  (:use midje.sweet)
  (:require [adi.schema.meta :as meta]))

^{:refer adi.schema.meta/schema-property :added "0.3"}
(fact "constructs a :db property out of a value and a type"
  (meta/schema-property :string :type) => :db.type/string
  (meta/schema-property :long :type) => :db.type/long
  (meta/schema-property :one :cardinality) => :db.cardinality/one
  (meta/schema-property :value :unique) => :db.unique/value)

^{:refer adi.schema.meta/attr-add-ident :added "0.3"}
(fact "adds the key of a pair as :ident to a schema property pair"
  (meta/attr-add-ident [:person [{}]])
  => [:person [{:ident :person}]]

  (meta/attr-add-ident [:person/address [{}]])
  => [:person/address [{:ident :person/address}]])

^{:refer adi.schema.meta/attr-add-defaults :added "0.3"}
(fact "adds defaults to a given schema property pair"
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
                :type :string}]])
