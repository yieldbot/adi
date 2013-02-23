(ns adi.test-query
  (:use midje.sweet
        adi.utils
        [adi.data :only [iid]])
  (:require [adi.data :as ad]
            [adi.schema :as as]
            [adi.query :as aq]
            [datomic.api :as d]))

(def *uri* "datomic:mem://test-adi-query")
(d/create-database *uri*)
;;  (d/delete-database *uri*)
(def *conn* (d/connect *uri*))

(def account-map
  (flatten-keys
   {:account
    {:username    [{:type        :string}]
     :hash        [{:type        :string}]
     :joined      [{:type        :instant}]
     :isActivated [{:type        :boolean
                    :default     :false}]
     :isVerified  [{:type        :boolean
                    :default     :false}]
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

(def account-info
  {:account
   {:username    "chris"
    :hash        "c27e8090a3ae"
    :joined      (java.util.Date.)
    :isActivated true
    :isVerified  true
    :firstName   "Chris"
    :lastName    "Zheng"
    :email       {:+ {:db/id (iid :main-email)}}
    :contacts    #{{:type :twitter :field "zc123"}
                   {:type :skype   :field "zcaudate"}
                   {:+ {:db/id (iid :main-email)}
                    :type :email   :field "z@caudate.me"}}

    :business
    {:name       "caudate inc."
     :abn        "389474839"
     :desc       "brain work"
     :industry   #{"internet" "computing"}}

    :address
    {:billing    {:+ {:db/id (iid :main-address)}}
     :shipping   {:+ {:db/id (iid :main-address)}}
     :all        #{{:+ {:db/id (iid :main-address)}
                    :country "Australia"
                    :region  "Victoria"
                    :city    "Melbourne"
                    :line1   "45 Greenways Rd"
                    :line2   ""
                    :postcode "3122"}}}}})

(d/transact *conn* (ad/generate-data account-map account-info))
(d/transact *conn* (as/generate-schemas account-map))

(aq/find-ids (d/db *conn*) {:account/username "chris"})
(seq (first (aq/find-entities (d/db *conn*) {:account/username "chris"})))
