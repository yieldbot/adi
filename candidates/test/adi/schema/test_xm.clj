(ns adi.schema.test-xm
  (:use midje.sweet
        adi.schema.xm)
  (:require [adi.schema.meta :as t]))

(fact "fschm-add-idents"
  (fschm-add-idents
   {:a/b [{}]
    :a/c [{}]})
  => {:a/b [{:ident :a/b}]
      :a/c [{:ident :a/c}]})

(fact "fschm-add-defaults"
  (fschm-add-defaults {:a [{}]})
  => {:a [{:cardinality :one :type :string}]}

  (fschm-add-defaults {:a [{:cardinality :many}]})
  => {:a [{:cardinality :many :type :string}]}

  (fschm-add-defaults {:a [{:ident :a
                        :type  :string
                        :index true
                        :fulltext true
                        :cardinality :many
                        :noHistory true}]} t/mschm-all-defaults)
  => {:a [{:ident :a
           :type  :string
           :index true
           :fulltext true
           :cardinality :many
           :noHistory true}]})

(fact
  (fschm-add-refs {:account/email [{:ident   :account/email
                                     :type    :ref
                                     :ref     {:ns  :email}}]})
  =>  {:email/accounts [{:ident :email/accounts
                         :cardinality :many
                         :type :ref
                         :ref {:ns :account
                               :type :reverse
                               :val :accounts
                               :key :account/_email
                               :rval :email
                               :rkey :account/email
                               :rident :account/email}}]
       :account/email [{:type :ref
                        :ident :account/email
                        :ref {:rident :email/accounts
                              :rkey :account/_email
                              :val :email
                              :key :account/email
                              :type :forward
                              :rval :accounts
                              :ns :email}}]})

(fact "fschm-prepare-ischm"
 (fschm-prepare-ischm {:account/email [{:ident   :account/email
                               :type    :ref
                               :ref     {:ns  :email}}]})
 =>  {:email/accounts [{:ident :email/accounts
                         :cardinality :many
                         :type :ref
                         :ref {:ns :account
                               :type :reverse
                               :val :accounts
                               :key :account/_email
                               :rval :email
                               :rkey :account/email
                               :rident :account/email}}]
      :account/email [{:ident :account/email
                       :cardinality :one
                       :type :ref
                       :ref {:rident :email/accounts
                              :rkey :account/_email
                              :val :email
                              :key :account/email
                              :type :forward
                              :rval :accounts
                              :ns :email}}]})
(fact "make-xm"
  (-> (make-xm {:account/email [{:ident   :account/email
                                 :type    :ref
                                 :ref     {:ns  :email}}]})
      :lu)
  =>  {:account/email :account/email
       :email/accounts :email/accounts
       :account/_email :email/accounts})
