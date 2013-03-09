(ns adi.api.test-02-emit-simple-data
 (:use midje.sweet
        adi.utils
        adi.checkers)
 (:require [adi.api :as aa]))

(def account-map
  (flatten-all-keys
   {:account
    {:username    [{:type        :string}]
     :hash        [{:type        :string}]
     :joined      [{:type        :instant}]
     :isActivated [{:type        :boolean
                    :default     false}]
     :isVerified  [{:type        :boolean
                    :default     false}]
     :firstName   [{:type        :string}]
     :lastName    [{:type        :string}]
     :email       [{:type        :ref
                    :ref-ns      :account.contact}]
     :contacts    [{:type        :ref
                    :ref-ns      :account.contact
                    :cardinality :many}]

     :business
     {:name       [{:type         :string}]
      :abn        [{:type         :string}]
      :desc       [{:type         :string}]
      :industry   [{:type         :string
                    :cardinality  :many}]}

     :address
     {:billing    [{:type        :ref
                    :ref-ns      :account.address}]
      :shipping   [{:type        :ref
                    :ref-ns      :account.address}]
      :all        [{:type        :ref
                    :ref-ns      :account.address
                    :cardinality :many}]}}

    :account.address
    {:country     [{:type        :string}]
     :region      [{:type        :string}]
     :city        [{:type        :string}]
     :line1       [{:type        :string}]
     :line2       [{:type        :string}]
     :postcode    [{:type        :string}]}

    :account.contact
    {:type        [{:type        :keyword}]
     :field       [{:type        :string}]}}))

(fact
  (aa/emit-insert {:account {:username "chris"}}
                  account-map)
  => (exclude-ids [{:account/username "chris" :account/isActivated false, :account/isVerified false}]))

(fact
  (aa/emit-update {:db/id 4
                   :account {:username "chris"}}
                  account-map)
  => [{:db/id 4, :account/username "chris"}])

(fact
  (aa/emit-query {:#/sym '?e
                  :account {:username "chris"}
                  :#/q [['?e :account/password "hello"]]}
                 account-map)
  => '[:find ?e :where [?e :account/username "chris"] [?e :account/password "hello"]])

(fact
  (aa/emit-refroute account-map [:account])
  =>
  #{:account/contacts :account/email :account/address/all
    :account/address/billing :account/address/shipping})
