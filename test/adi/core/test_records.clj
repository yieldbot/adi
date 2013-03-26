(ns adi.core.test-records
  (:use midje.sweet
        adi.utils
        [adi.data :only [iid]])
  (:require [adi.core :as adi]
            [datomic.api :as d]))

(def ^:dynamic *ds*
  (adi/datastore "datomic:mem://test-adi-core"
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
                            :desc       [{:type         :string
                                          :fulltext     true}]
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
                           :field       [{:type        :string}]}}
                         true true))

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

(def account-info-2
  {:account
   {:username    "titan"
    :hash        "c27e8090a3ae"
    :joined      (java.util.Date.)
    :isActivated true
    :isVerified  true
    :firstName   "Chris"
    :lastName    "Strong"
    :email       {:+ {:db/id (iid :main-email)}}
    :contacts    #{{:type :twitter :field "titicular"}
                   {:type :skype   :field "titicular"}
                   {:+ {:db/id (iid :main-email)}
                    :type :email   :field "titi@cul.ar"}}

    :business
    {:name       "titicular"
     :abn        "38923433"
     :desc       "muscles work"
     :industry   #{"moving" "storms"}}

    :address
    {:billing    {:+ {:db/id (iid :main-address)}}
     :shipping   {:+ {:db/id (iid :main-address)}}
     :all        #{{:+ {:db/id (iid :main-address)}
                    :country "Australia"
                    :region  "Victoria"
                    :city    "Melbourne"
                    :line1   "101 Olympus Dr"
                    :line2   ""
                    :postcode "3000"}}}}})

(adi/insert! account-info *ds*)
(adi/insert! account-info-2 *ds*)


(fact "common searchesn"
  (adi/select {:account/firstName "Chris"} *ds*)
  => (fn [x] (= 2 (count x)))

  (adi/select {:account {:address {:all {:country "Australia"}}}}  *ds*)

  (adi/select {:account.address {:country "Australia"}}  *ds*)
  => (fn [x] (= 2 (count x)))

  (adi/select{:account.contact/type '_} *ds*)
  => (fn [x] (= 6 (count x)))

  (adi/select {:account.contact/type :email} *ds*)
  => (fn [x] (= 2 (count x))))


(fact "searches across refs"
  (adi/select {:account/email/field "z@caudate.me"} *ds*)
  => (fn [x] (= 1 (count x)))

  (adi/select {:account/address/all/city "Melbourne"} *ds*)
  => (fn [x] (= 2 (count x)))

  (adi/select {:account/address/billing/city "Melbourne"} *ds*)
  => (fn [x] (= 2 (count x)))

  (= (adi/select {:account/address/billing/postcode "3000"} *ds*)
     (adi/select {:account/address/shipping/postcode "3000"} *ds*)
     (adi/select {:account/address/all/postcode "3000"} *ds*))
  => true)

(fact "fulltext searches"
  (adi/select {:#/fulltext {:account/business/desc "brain"}}  *ds*)
  => (fn [x] (= 1 (count x)))

  (adi/select {:#/fulltext {:account/business/desc "work"}}  *ds*)
  => (fn [x] (= 2 (count x)))

    (d/q '[:find ?e :where
         [(fulltext $ :account/business/desc "work") [[?e ?ft1]]]]
       (d/db (:conn *ds*)))
    => (fn [x] (= 2 (count x))))
