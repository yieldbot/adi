(ns adi.test-schema
  (:use midje.sweet
        adi.utils
        adi.checkers)
  (:require [adi.schema :as as]))

(fact "add-idents will take a scheme-map and automatically
       adds the :ident keyword"

    (as/add-idents     {:account {:name [{:k1 :v1 :k2 :v2 :k3 :v3}]
                                  :age  [{:k1 :v1 :k2 :v2 :k3 :v3}]}})
    =>  {:account {:age  [{:ident :account/age, :k1 :v1, :k2 :v2, :k3 :v3}],
                   :name [{:ident :account/name, :k1 :v1, :k2 :v2, :k3 :v3}]}}

    (as/add-idents     {:account/name [{:k1 :v1 :k2 :v2 :k3 :v3}]
                        :account/age  [{:k1 :v1 :k2 :v2 :k3 :v3}]})
    => {:account {:age  [{:ident :account/age, :k1 :v1, :k2 :v2, :k3 :v3}],
                  :name [{:ident :account/name, :k1 :v1, :k2 :v2, :k3 :v3}]}})


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
               :required true
               :ref-ns      :link}]
      :value [{:type        :string
               :default     "undefined"}]}})

(fact "actual examples of usage of linearise."
  (flatten-all-keys (as/add-idents account-map))
  => {:account/username  [{:ident :account/username,
                            :unique :value,
                            :type :string,
                            :doc "The username associated with the account"}]
      :account/password [{:ident :account/password,
                           :type :string,
                           :doc "The password associated with the account"}]}

    (flatten-all-keys (as/add-idents link-map))
    => {:link/next [{:ident :link/next
                     :required true
                     :type :ref,
                     :ref-ns :link}]
        :link/value [{:ident :link/value
                       :type :string
                       :default "undefined"}]})

(fact "type-checks store the checker associated with the keyword"
  (as/geni-type-checks :keyword) => (midje.sweet/exactly (resolve 'keyword?))
  (as/geni-type-checks :uuid) => (midje.sweet/exactly (resolve 'uuid?)))

(fact "build-schema* takes a property map and converts it into a datomic schema"
  (#'as/build-schema*  {:ident :account/password,
                          :type :string,
                          :doc "The password associated with the account"})
    =>(exclude-id
       {:db.install/_attribute :db.part/db,
        :db/ident :account/password,
        :db/doc "The password associated with the account",
        :db/valueType :db.type/string,
        :db/cardinality :db.cardinality/one})

    (#'as/build-schema* {:ident :link/next :type :ref :ref-ns :link})
    => (exclude-id
        {:db.install/_attribute :db.part/db
         :db/ident :link/next
         :db/valueType :db.type/ref
         :db/cardinality :db.cardinality/one})

    (#'as/build-schema* {:ident :link/tag :type :string :cardinality :many})
    => (exclude-id
        {:db.install/_attribute :db.part/db
         :db/ident :link/tag
         :db/valueType :db.type/string
         :db/cardinality :db.cardinality/many}))

(fact
  (as/build-schema account-map)
  => (exclude-ids
      [{:db/cardinality :db.cardinality/one,
         :db/doc "The password associated with the account",
         :db/ident :account/password,
         :db/valueType :db.type/string,
         :db.install/_attribute :db.part/db}
        {:db/cardinality :db.cardinality/one,
         :db/doc "The username associated with the account",
         :db/ident :account/username,
         :db/unique :db.unique/value,
         :db/valueType :db.type/string,
         :db.install/_attribute :db.part/db}]))



(fact
  (as/find-required-keys
   (flatten-keys (as/add-idents (merge account-map link-map)))
   #{:link})
  => #{:link/next}

  (as/find-required-keys
   (flatten-keys (as/add-idents (merge account-map link-map)))
   #{:account})
  => #{})
