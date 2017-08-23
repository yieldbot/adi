(ns spirit.datomic.schema.base-test
  (:use hara.test)
  (:require [spirit.datomic.schema.base :as base :refer :all]
            [spirit.data.schema.ref :as ref]
            [spirit.data.schema.base :as common]))

^{:refer spirit.data.schema.base/schema-property :added "0.3"}
(fact "constructs a :db property out of a value and a type"
  (schema-property :string :type) => :db.type/string
  (schema-property :long :type) => :db.type/long
  (schema-property :one :cardinality) => :db.cardinality/one
  (schema-property :value :unique) => :db.unique/value)
