(ns adi.schema.test-ref
  (:use midje.sweet
        adi.schema.ref)
  (:require [adi.schema.ref :as t]))

(fact "keyword-reverse"
  (keyword-reverse :b) => :_b
  (keyword-reverse :_b) => :b
  (keyword-reverse :a/b) => :a/_b
  (keyword-reverse :a/_b) => :a/b
  (keyword-reverse :a/b/c) => :a/b/_c
  (keyword-reverse :a/b/_c) => :a/b/c)

(fact "keyword-reverse?"
  (keyword-reversed? :a) => false
  (keyword-reversed? :a/b) => false
  (keyword-reversed? :a/_b) => true
  (keyword-reversed? :a/b/_c) => true)

(fact "is-reversible-attr?"
  (is-reversible-attr? [{:ident   :account/email    ;; okay
                     :type    :ref
                     :ref     {:ns  :email}}])
  => true

  (is-reversible-attr? [{:ident   :account/email}]) ;; is missing data
  => false

  (is-reversible-attr? [{:ident   :email            ;; does not have keyword-ns
                     :type    :ref
                     :ref     {:ns  :email}}])
  => false

  (is-reversible-attr? [{:ident   :account/_email   ;; is already reversible
                     :type    :ref
                     :ref     {:ns  :email}}])
  => false

  (is-reversible-attr? [{:ident   :account/email    ;; is not a ref
                     :type    :long
                     :ref     {:ns  :email}}])
  => false

  (is-reversible-attr? [{:ident   :account/email    ;; is tagged 'norev'
                     :type    :ref
                     :ref     {:ns  :email
                               :norev true}}])
  => false)


(fact "find-ref-attrs"
  (find-ref-attrs {:a [{:type   :ref}]})
  => {:a [{:type   :ref}]}

  (find-ref-attrs {:a [{:type   :ref}]
                   :b [{:type   :long}]})
  => {:a [{:type   :ref}]})


(fact "find-ref-idents"
  (find-ref-idents {:a [{:type   :ref}]})
  => [:a]

  (find-ref-idents {:a [{:type   :ref}]
                    :b [{:type   :long}]})
  => [:a]

  (find-ref-idents {:a [{:type   :ref}]
                    :b [{:type   :ref}]})
  => (just [:a :b] :in-any-order)

  (find-ref-idents {:a/b [{:type   :ref}]
                    :c   [{:type   :long}]})
  => [:a/b])


(fact "determine-ref-rval"
  (determine-ref-rval [[:account :email false]
                   [{:ident  :account/email}]])
  => :accounts

  (determine-ref-rval [[:account :email true]
                   [{:ident  :account/email}]])
  => :email_accounts

  (determine-ref-rval [[:account :image true]
                   [{:ident  :account/bigImage}]])
  => :bigImage_accounts

  (determine-ref-rval [[:account :image true]
                   [{:ident  :account/big/image}]])
  => :big_image_accounts

  (determine-ref-rval [[:node  :node  true]
                    [{:ident  :node/children
                      :ref    {:ns    :node}}]])
  => :children_of

  (determine-ref-rval [[:node  :node  false]
                   [{:ident  :node/children
                     :ref    {:ns    :node}}]])
  => :children_of

  (determine-ref-rval [[:node  :node  false]
                   [{:ident  :node/children
                     :ref    {:ns    :node
                              :rval  :parents}}]])
  => :parents)


(fact "prepare-forward-attr"
  (prepare-forward-attr [{:ident  :node/children
                    :ref    {:ns    :node}}])
  => (throws Exception) ;; needs rval to be there


  (prepare-forward-attr [{:ident  :node/children
                      :ref    {:ns    :node
                               :rval  :parents}}])
  => [{:ident    :node/children
       :ref      {:ns     :node
                  :type   :forward
                  :val    :children
                  :key    :node/children
                  :rval   :parents
                  :rkey   :node/_children
                  :rident :node/parents}}])

(fact "prepare-reverse-attr"
  (prepare-reverse-attr [{:ident    :node/children
                       :ref      {:ns     :node
                                  :type   :forward
                                  :val    :children
                                  :key    :node/children
                                  :rval   :parents
                                  :rkey   :node/_children
                                  :rident :node/parents}}])
  => [{:ident :node/parents
        :cardinality :many
        :type :ref
        :ref  {:ns      :node
               :type    :reverse
               :val     :parents
               :key     :node/_children
               :rval    :children
               :rkey    :node/children
               :rident  :node/children}}]

  (prepare-reverse-attr [{:ident    :node/children
                       :ref      {:ns     :node}}])
  => (throws Exception))

(fact "prepare-ref-attr"
  (prepare-ref-attr [[:node  :node  false]
               [{:ident  :node/children
                 :ref    {:ns    :node}}]])
  => [{:ident :node/children
       :ref   {:ns     :node
               :type   :forward
               :val    :children
               :key    :node/children
               :rval   :children_of
               :rkey   :node/_children
               :rident :node/children_of}}])

(fact "attr-ref-ns-pair"
  (attr-ref-ns-pair [{:ident  :a/b
                      :ref    {:ns :c}}])
  => [:a :c]

  (attr-ref-ns-pair [{:ident  :a/b/c
                      :ref    {:ns :d}}])
  => [:a :d])



(fact "prepare-all-ref-attrs"
  (prepare-all-ref-attrs {:account/email [{:ident   :account/email
                                            :type    :ref
                                            :ref     {:ns  :email}}]})
  => [[{:type  :ref
        :ident :account/email
        :ref   {:ns      :email
                :type    :forward
                :val     :email
                :key     :account/email
                :rkey    :account/_email
                :rval    :accounts
                :rident  :email/accounts}}]])
