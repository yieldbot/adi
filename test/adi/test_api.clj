(ns adi.test-query
  (:use midje.sweet
        adi.utils
        [adi.data :only [iid]])
  (:require [adi.data :as ad]
            [adi.schema :as as]
            [adi.api :as aa]
            [datomic.api :as d]))

(def *uri* "datomic:mem://test-adi-query")
(d/create-database *uri*)
(comment (d/delete-database *uri*))
(def *conn* (d/connect *uri*))

(def account-map
  (flatten-keys
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

@(d/transact *conn* (as/emit-schema account-map))
@(d/transact *conn* (ad/emit account-map account-info))
@(d/transact *conn* (ad/emit account-map account-info-2))

(def link-map
  (flatten-keys
   {:link {:next  [{:type        :ref
                    :ref-ns      :link}]
           :value [{:type        :string
                    :default     "undefined"}]}}))

(def link-data
  {:link {:value "1"
          :next {:value "2"
                 :next  {:value "3"
                         :next {:value "4"}}}}})

(def link-circular
  {:db/id (iid :start)
   :link {:value "1C"
          :next {:value "2C"
                 :next  {:value "3C"
                         :next {:value "4C"
                                :next {:+ {:db/id (iid :start)}}}}}}})


@(d/transact *conn* (as/emit-schema link-map))
@(d/transact *conn* (ad/emit link-map link-data))
@(d/transact *conn* (ad/emit link-map link-circular))


(comment
  (d/entity (d/db *conn*) chris)

  (aa/delete-linked! *conn* link-map {:link/value "2"} #{:link/_next})
  (aa/delete-linked! *conn* link-map {:link/value "2C"} #{:link/next})


  (aa/update! *conn* account-map
              {:account/firstName "Chris"} {:account {:lastName "Hello"}})

  (aa/update! *conn* account-map
              chris {:account {:lastName "Zheng"}})

  (aa/all-ref-ids
   (aa/select-first-entity (d/db *conn*) {:link/value "1"}) #{:link/next})
  (aa/all-ref-ids
   (aa/select-first-entity (d/db *conn*) {:link/value "4C"}) #{:link/next})

  (assoc (aa/select-first-entity (d/db *conn*) {:link/value "4C"}) :e 3)

  (aa/retract! *conn*
               {:account/firstName "Chris"} #{:account/lastName :account/username}))



(d/basis-t (d/db *conn*))

(d/tx-report-queue *conn*)

(def data (seq (d/datoms (d/db *conn*) :eavt)))

(pprint data)

(doc d/datoms)
(->
 (map #(d/entity (d/db *conn*) (first %))
      (d/q '[:find ?tx ?when :where [?tx :db/txInstant ?when]]
           (d/db *conn*)))
 first keys)

(def dbs
  (d/q
   '[:find ?e ?v ?time
     :where
     [?e ?a ?v ?time]
     [?a :db/ident :account/username]]
   (d/db *conn*)))

(d/q
   '[:find ?e ?v ?added
     :where
     [?e ?a ?v _ ?added]
     [?a :db/txInstan _]]
   (d/db *conn*))

(require '[adi.history :as ah])

(ah/time-point (d/db *conn*) 17592186045423 :account/username)
(ah/time-point (d/db *conn*) (d/entity (d/db *conn*) 17592186045423) :account/username)

(aa/select-ids (d/db *conn*) {:account/firstName "Chris"})

(d/basis-t (d/db *conn*))
(d/since (d/db *conn*) )

(ah/time-point (d/db *conn*) 17592186045423 :account/username)

(ah/attr-tx (d/db *conn*) 17592186045423 :account/username)

(ah/attr-tx-prev (d/db *conn*) 17592186045423 :account/username)
(ah/attr-tx (d/db *conn*) 17592186045423 :account/username)
(ah/attr-tx (d/db *conn*) 17592186045423 :account/lastName)
(ah/attr-tx-prev (d/db *conn*) 17592186045423 :account/lastName)
(ah/attr-tx-first (d/db *conn*) 17592186045423 :account/username)

(ah/tx (d/db *conn*) 17592186045423)

(def tx1 (ah/attr-tx (d/db *conn*) 17592186045423 :account/lastName))

(def dbt1 (d/as-of (d/db *conn*) (dec (first tx1))))

(def tx2 (ah/attr-tx dbt1 17592186045423 :account/lastName))

(d/datoms (d/db *conn*))

(ad/list-ns )
(aa/db-attrs (d/db *conn*) :account)

(aa/db-doc (d/db *conn*) :account/lastName)
(aa/db-txs (d/db *conn*))

(comment
  (def chris (first (aa/select-ids (d/db *conn*) {:account/firstName "Chris"})))
  (:account/lastName (aa/select-first-entity (d/db *conn*) {:account/firstName "Chris"}))
  (d/transact *conn* [[:db/add chris :account/lastName "Mattruso"]])
  (d/transact *conn* [[:db/add chris :account/lastName "Zheng"]])
  (d/transact *conn* [[:db/retract chris :account/lastName "Zheng"]])
  (d/transact *conn* [[:db/retract chris :account/lastName "Mattruso"]]))

(comment
  (aa/select-ids (d/db *conn*) {:account/firstName "Chris"})
  (aa/select-ids (d/db *conn*) {:account.address/city '_})

  (aa/select-ids (d/db *conn*)
               (aa/select-entities (d/db *conn*) {:account/firstName "Chris"}))

  (aa/all-ref-ids
   (aa/select-first-entity (d/db *conn*) {:account/firstName "Chris"})
   #{:account/email})

  (pprint
   (ad/unprocess account-map
                      (aa/select-first-entity (d/db *conn*) {:account/firstName "Chris"})))

  (aa/delete! *conn* {:account/firstName "Chris"})

  (aa/delete-linked! *conn* account-map {:account/firstName "Chris"} #{:account/email})


  (type (first (aq/select-entities (d/db *conn*) {:account/firstName "Chris"}))))



(seq (first (aq/select-entities (d/db *conn*) {:account/firstName "Chris"})))


(d/q '[:find ?tx ?tx-time ?v
       :in $ ?e ?a
       :where [?e ?a ?v ?tx _]
       [?tx :db/txInstant ?tx-time]]
     (d/history (d/db *conn*))
     17592186045423
     :account/lastName)

(pprint
 (->> (d/q '[:find ?tx ?v ?added
             :in $ ?e ?a
             :where [?e ?a ?v ?tx ?added]
             [?tx :db/txInstant ?tx-time]]
           (d/history (d/db *conn*))
           17592186045423
           :account/lastName)
      (sort #(compare (first %1) (first %2)))
      ;;set
      ))
(d/basis-t (d/db *conn*))

(d/t->tx 10)


(d/history (d/db *conn*))
