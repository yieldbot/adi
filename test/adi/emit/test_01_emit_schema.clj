(ns adi.emit.test-01-emit-schema
 (:use midje.sweet
        adi.utils
        adi.checkers)
  (:require [adi.emit :as ae]))

(fact "emit-schema takes a scheme-map and turns it into a schema
       that is installable into datomic"
  (ae/emit-schema
   {:account
    {:username  [{:type        :string
                  :unique      :value
                  :doc         "The username associated with the account"}]
     :password  [{:type        :string
                  :doc         "The password associated with the account"}]}})
  => (exclude-ids [{:db.install/_attribute :db.part/db,
                    :db/ident :account/password,
                    :db/doc "The password associated with the account",
                    :db/valueType :db.type/string,
                    :db/cardinality :db.cardinality/one}
                   {:db.install/_attribute :db.part/db,
                    :db/ident :account/username,
                    :db/doc "The username associated with the account",
                    :db/valueType :db.type/string,
                    :db/unique :db.unique/value,
                    :db/cardinality :db.cardinality/one}])

  (ae/emit-schema
   {:link
    {:next  [{:type :ref :ref-ns :link}]
     :value [{:type :string :default "undefined"}]}})
  => (exclude-ids [{:db.install/_attribute :db.part/db,
                    :db/ident :link/next,
                    :db/valueType :db.type/ref,
                    :db/cardinality :db.cardinality/one}
                   {:db.install/_attribute :db.part/db,
                    :db/ident :link/value,
                    :db/valueType :db.type/string,
                    :db/cardinality :db.cardinality/one}])

  (ae/emit-schema {"name" [{:type :ref}]})
  => (exclude-ids [{:db.install/_attribute :db.part/db,
                    :db/ident :name,
                    :db/valueType :db.type/ref,
                    :db/cardinality :db.cardinality/one}])

  ;; full schema
  (ae/emit-schema {:name [{:type :string
                           :cardinality :many
                           :unique :value
                           :doc "The name of something"
                           :index true
                           :fulltext true
                           :no-history true
                           :OTHERS :WILL-NOT-SHOW}]})
  => (exclude-ids [{:db.install/_attribute :db.part/db,
                    :db/ident :name
                    :db/valueType :db.type/string
                    :db/cardinality :db.cardinality/many
                    :db/unique :db.unique/value
                    :db/doc "The name of something"
                    :db/index true
                    :db/fulltext true
                    :db/no-history true}]))

(fact "emit-schema exceptions. The function should blow up when:"
   (ae/emit-schema {:name [{}]})
  => (throws Exception)

  (ae/emit-schema {:name [{:type :wrong-type}]})
  => (throws Exception)

  (ae/emit-schema {:name [{:type :ref
                           :doc :NOT-STRING}]})
  => (throws Exception)

  (ae/emit-schema {:name [{:type :ref
                           :cardinality :not-one-or-many}]})
  => (throws Exception)

  (ae/emit-schema {:name [{:type :ref
                           :unique :not-value-or-identity}]})
  => (throws Exception)

  (ae/emit-schema {:name [{:type :ref
                           :index :not-a-bool-value}]})
  => (throws Exception))
