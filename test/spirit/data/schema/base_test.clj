(ns spirit.data.schema.base-test
  (:use hara.test)
  (:require [spirit.data.schema.base :as base]))

^{:refer spirit.data.schema.base/attr-add-ident :added "0.3"}
(fact "adds the key of a pair as :ident to a schema property pair"
  (base/attr-add-ident [:person [{}]])
  => [:person [{:ident :person}]]

  (base/attr-add-ident [:person/address [{}]])
  => [:person/address [{:ident :person/address}]])

^{:refer spirit.data.schema.base/attr-add-defaults :added "0.3"}
(fact "adds defaults to a given schema property pair"
  (base/attr-add-defaults [:person [{}]] [])
  => [:person [{}]]

  (base/attr-add-defaults [:person [{}]]
                          [{:default :string, :auto true, :id :type} 
                           {:default :one, :auto true, :id :cardinality}])
  => [:person [{:cardinality :one :type :string}]]

  (base/attr-add-defaults [:person [{:cardinality :many :type :long}]]
                          [{:default :string, :auto true, :id :type} 
                           {:default :one, :auto true, :id :cardinality}])
  => [:person [{:cardinality :many
                :type :long}]]

  (base/attr-add-defaults [:person [{}]]
                          [{:default false, :id :index} 
                           {:default false, :id :fulltext} 
                           {:default false, :id :noHistory} 
                           {:default :string, :auto true, :id :type} 
                           {:default :one, :auto true, :id :cardinality}])
  => [:person [{:index false
                :fulltext false
                :cardinality :one
                :noHistory false
                :type :string}]])
