(ns adi.test-schema
  (:use midje.sweet
        adi.utils
        adi.checkers)
  (:require [adi.schema :as as]))

;; Basic Tests

(fact "linearise-schm will take a scheme-map and transforms
       the result into turn it into a linear-schema"
  (as/linearise-schm {:name [{:type :string}]})
  => [{:ident :name :type  :string}]

  (as/linearise-schm {:name [{:type :string :k1 :v1 :k2 :v2 :k3 :v3}]})
  => [{:ident :name :type  :string  :k1 :v1 :k2 :v2 :k3 :v3}]

  (as/linearise-schm {:name [{:k1 :v1 :k2 :v2 :k3 :v3}]
                      :age  [{:k1 :v1 :k2 :v2 :k3 :v3}]})
  => [{:ident :name :k1 :v1 :k2 :v2 :k3 :v3}
      {:ident :age  :k1 :v1 :k2 :v2 :k3 :v3}]

  (as/linearise-schm {:account {:name [{:k1 :v1 :k2 :v2 :k3 :v3}]
                                :age  [{:k1 :v1 :k2 :v2 :k3 :v3}]}})
  => [{:ident :account/name :k1 :v1 :k2 :v2 :k3 :v3}
      {:ident :account/age  :k1 :v1 :k2 :v2 :k3 :v3}]

  (as/linearise-schm {:account/name [{:k1 :v1 :k2 :v2 :k3 :v3}]
                      :account/age  [{:k1 :v1 :k2 :v2 :k3 :v3}]})
  => [{:ident :account/name :k1 :v1 :k2 :v2 :k3 :v3}
      {:ident :account/age  :k1 :v1 :k2 :v2 :k3 :v3}])

(def account-map
  {:account
   {:username  [{:type        :string
                 :unique      :value
                 :doc         "The username associated with the account"}]
    :password  [{:type        :string
                 :doc         "The password associated with the account"}]}})

(def link-map
  {:link
   {:next  [{:type        :ref
             :ref-ns      :link}]
    :value [{:type        :string
             :default     "undefined"}]}})


(fact "actual examples of usage of linearise-schm."
  (set (as/linearise-schm account-map))
  => (set [{:ident :account/username,
            :unique :value,
            :type :string,
            :doc "The username associated with the account"}
           {:ident :account/password,
            :type :string,
            :doc "The password associated with the account"}])

  (set (as/linearise-schm link-map))
  => (set [{:ident :link/next
            :type :ref,
            :ref-ns :link}
           {:ident :link/value
            :type :string
            :default "undefined"}]))


(fact "type-checks store the checker associated with the keyword"
  (as/type-checks :keyword) => (midje.sweet/exactly (resolve 'keyword?))
  (as/type-checks :uuid) => (midje.sweet/exactly (resolve 'uuid?)))

(fact "lschm->schema takes a property map and converts it into a datomic schema"
  (#'as/lschm->schema  {:ident :account/password,
                        :type :string,
                        :doc "The password associated with the account"})
  =>(exclude-id
     {:db.install/_attribute :db.part/db,
      :db/ident :account/password,
      :db/doc "The password associated with the account",
      :db/valueType :db.type/string,
      :db/cardinality :db.cardinality/one})

  (#'as/lschm->schema {:ident :link/next :type :ref :ref-ns :link})
  => (exclude-id
      {:db.install/_attribute :db.part/db
       :db/ident :link/next
       :db/valueType :db.type/ref
       :db/cardinality :db.cardinality/one})

  (#'as/lschm->schema {:ident :link/tag :type :string :cardinality :many})
  => (exclude-id
      {:db.install/_attribute :db.part/db
       :db/ident :link/tag
       :db/valueType :db.type/string
       :db/cardinality :db.cardinality/many}))
