(ns spirit.common.schema-test
  (:use hara.test)
  (:require [spirit.common.schema :refer :all]))

^{:refer spirit.common.schema/simplify :added "0.3"}
(fact "helper function for easier display of spirit schema"
  (simplify {:account/name  [{:type :long}]
             :account/email [{:type :string
                              :cardinality :many}]
             :email/accounts [{:type :ref
                               :cardinality :many
                               :ref {:ns :account}}]})
  => {:email {:accounts :&account<*>}
      :account {:email :string<*> :name :long}})

^{:refer spirit.common.schema/create-lookup :added "0.3"}
(fact "lookup from flat schema mainly for reverse refs"
  (create-lookup
   {:account/name   [{}]
    :account/email  [{}]
    :email/accounts [{:ident :email/accounts
                      :type :ref
                      :ref {:type :reverse
                            :key :account/_email}}]})
  => {:email/accounts :email/accounts
      :account/_email :email/accounts
      :account/email :account/email
      :account/name :account/name})

^{:refer spirit.common.schema/create-flat-schema :added "0.3"}
(fact "creates a flat schema from an input map"
  (create-flat-schema {:account {:email [{:type    :ref
                                          :ref     {:ns  :email}}]}})
  => {:email/accounts [{:ident :email/accounts
                        :type :ref
                        :cardinality :many
                        :ref {:ns :account
                              :type :reverse
                              :key :account/_email
                              :val :accounts
                              :rval :email
                              :rkey :account/email
                              :rident :account/email}}]
      :account/email [{:ident :account/email
                       :type :ref
                       :cardinality :one
                       :ref  {:ns :email
                              :type :forward
                              :key :account/email
                              :val :email
                              :rval :accounts
                              :rkey :account/_email
                              :rident :email/accounts}}]})

^{:refer spirit.common.schema/schema :added "0.3"}
(fact "creates an extended schema for use by spirit"
  (-> (schema {:account/name   [{}]
               :account/email  [{:ident   :account/email
                                 :type    :ref
                                 :ref     {:ns  :email}}]})
      :flat
      simplify)
  => {:email {:accounts :&account<*>}
      :account {:email :&email
                :name :string}})
