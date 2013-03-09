(ns adi.test-api
  (:use midje.sweet
        adi.utils
        [adi.data :only [iid]])
  (:require [adi.data :as ad]
            [adi.schema :as as]
            [adi.api :as aa]
            [datomic.api :as d]))

(def ^:dynamic *uri* "datomic:mem://test-adi-api")
(def ^:dynamic *conn* (aa/connect! *uri* true))

(iid :eueu)
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

#_(aa/filter-empty-refs [{:db/id 1}])

#_(aa/filter-empty-refs (aa/emit-insert account-info-2 account-map))
#_(aa/emit-update account-info-2 account-map)


@(aa/install-schema *conn* account-map)
#_(aa/insert! *conn* account-map {:account
                                 {:username    "titan"
                                  :hash        "c27e8090a3ae"
                                  :joined      (java.util.Date.)}})
@(aa/insert! *conn* account-map account-info)
@(aa/insert! *conn* account-map account-info-2)

(aa/select-ids (d/db *conn*) account-map {:account/username "titan"})

(aa/select-entities (d/db *conn*) account-map {:account/username "titan"})

(aa/select (d/db *conn*) account-map {:account/username "titan"})

(aa/select (d/db *conn*) account-map {:account/username "titan"}
           (aa/emit-refroute account-map))

(aa/select-ids (d/db *conn*) account-map {:account {:address {:all {:country "Australia"}}
                                                    :contacts {:field "zc123"}}})


(aa/select (d/db *conn*) account-map '(:find ?e :where [?e :account/username "titan"])
           (aa/emit-refroute account-map))


(comment (d/delete-database *uri*))
