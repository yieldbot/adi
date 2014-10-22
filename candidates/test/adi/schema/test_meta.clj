(ns adi.schema.test-meta
  (:use midje.sweet
        adi.schema.meta)
  (:require [adi.schema.meta :as t]))

(fact "meta-schema-property"
  (meta-schema-property :string :type) => :db.type/string
  (meta-schema-property :long :type) => :db.type/long
  (meta-schema-property :one :cardinality) => :db.cardinality/one
  (meta-schema-property :value :unique) => :db.unique/value)


(fact "mschm-attr-add-ident"
  (mschm-attr-add-ident [:a [{}]]) => [:a [{:ident :a}]]
  (mschm-attr-add-ident [:a/b/c [{}]]) => [:a/b/c [{:ident :a/b/c}]])

(fact "mschm-attr-add-defaults"
  (mschm-attr-add-defaults [:a [{}]] [])
  => [:a [{}]]

  (mschm-attr-add-defaults [:a [{}]] mschm-all-auto-defaults)
  => [:a [{:cardinality :one :type :string}]]

  (mschm-attr-add-defaults [:a [{:cardinality :many :type :long}]]
                     mschm-all-auto-defaults)
  => [:a [{:cardinality :many :type :long}]]

  (mschm-attr-add-defaults [:a [{}]] mschm-all-defaults)
  => [:a [{:index false, :fulltext false,
           :cardinality :one, :noHistory false
           :type :string}]])
