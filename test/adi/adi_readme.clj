(ns adi.adi-readme
  (:use adi.utils)
  (:require [adi.core :as adi]
            [adi.schema :as as]
            [adi.data :as ad]))

(def sm-account
  {:account
   {:username     [{:type        :string}]
    :password     [{:type        :string}]
    :permissions  [{:type        :keyword
                    :cardinality :many}]
    :points       [{:type        :long
                    :default     0}]
    :socialMedia  [{:type        :ref
                    :ref-ns      :account.social
                    :cardinality :many}]}
   :account.social
   {:type         [{:type        :keyword}]
    :name         [{:type        :string}]}})


(pprint (as/emit-schema sm-account))
(pprint (flatten-keys sm-account))

(def data-account
  [{:account {:username "alice"
              :password "a123"
              :permissions #{:member}}}
   {:account {:username "bob"
              :password "b123"
              :permissions #{:admin}
              :socialMedia #{{:type :facebook :name "bob@facebook.com"}
                             {:type :twitter :name "bobtwitter"}}}}
   {:account {:username "charles"
              :password "b123"
              :permissions #{:member :editor}
              :socialMedia #{{:type :facebook :name "charles@facebook.com"}
                             {:type :twitter :name "charlestwitter"}}
              :points 1000}}
   {:account {:username "dennis"
              :password "d123"
              :permissions #{:member}}}
   {:account {:username "elaine"
              :password "e123"
              :permissions #{:editor}
              :points 100}}
   {:account {:username "fred"
              :password "f123"
              :permissions #{:member :admin :editor}
              :points 5000
              :socialMedia #{{:type :facebook :name "fred@facebook.com"}}}}])

(pprint (apply ad/emit (:fsm ds) data-account))

(def ds (adi/datastore sm-account "datomic:mem://adi-example" true))

(apply adi/insert! ds data-account)

(adi/select-entities ds {:account/username "alice"})
(pprint (adi/select ds {:account/username "bob"} #{:account/socialMedia}))

(ad/process (:fsm ds) (ad/unprocess (:fsm ds) (first (adi/select-entities ds {:account/username "alice"})) #{}))


(ad/unprocess (adi/select-entities ds {:account/username "alice"}))


(println (:conn ds))

(pprint
 (adi/query ds
            '[:find ?e ?name
              :where
              [?e :account/username ?name]]))


(pprint (adi/select ds 17592186045421))


(pprint (adi/select ds {:account/permissions :editor}))

(pprint (adi/select ds {:db/txInstant '_}))
