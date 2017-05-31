(ns spirit.common.schema.base-test
  (:use hara.test)
  (:require [spirit.common.schema.base :as base]
            [spirit.datomic.schema.base :as datomic]))

^{:refer spirit.common.schema.base/attr-add-ident :added "0.3"}
(fact "adds the key of a pair as :ident to a schema property pair"
  (base/attr-add-ident [:person [{}]])
  => [:person [{:ident :person}]]

  (base/attr-add-ident [:person/address [{}]])
  => [:person/address [{:ident :person/address}]])

^{:refer spirit.common.schema.base/attr-add-defaults :added "0.3"}
(fact "adds defaults to a given schema property pair"
  (base/attr-add-defaults [:person [{}]] [])
  => [:person [{}]]

  (base/attr-add-defaults [:person [{}]]
                          datomic/all-auto-defaults)
  => [:person [{:cardinality :one :type :string}]]

  (base/attr-add-defaults [:person [{:cardinality :many :type :long}]]
                          datomic/all-auto-defaults)
  => [:person [{:cardinality :many
                :type :long}]]

  (base/attr-add-defaults [:person [{}]]
                          datomic/all-defaults)
  => [:person [{:index false
                :fulltext false
                :cardinality :one
                :noHistory false
                :type :string}]])
