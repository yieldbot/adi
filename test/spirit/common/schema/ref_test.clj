(ns spirit.common.schema.ref-test
  (:use hara.test)
  (:require [spirit.common.schema.ref :refer :all]))

^{:refer spirit.common.schema.ref/is-reversible? :added "0.3"}
(fact "determines whether a ref attribute is reversible or not"
  (is-reversible? {:ident   :account/email    ;; okay
                   :type    :ref
                   :ref     {:ns  :email}})
  => true

  (is-reversible? {:ident   :email            ;; does not have keyword-ns
                   :type    :ref
                   :ref     {:ns  :email}})
  => false
  ^:hidden
  (is-reversible? {:ident   :account/email}) ;; is missing data
  => false


  (is-reversible? {:ident   :account/_email   ;; is already reversible
                   :type    :ref
                   :ref     {:ns  :email}})
  => false

  (is-reversible? {:ident   :account/email    ;; is not a ref
                   :type    :long
                   :ref     {:ns  :email}})
  => false

  (is-reversible? {:ident   :account/email    ;; is tagged 'norev'
                   :type    :ref
                   :ref     {:ns  :email
                             :norev true}})
  => false)

^{:refer spirit.common.schema.ref/determine-rval :added "0.3"}
(fact "outputs the :rval value of a :ref schema reference"

  (determine-rval [[:account :email false]
                   [{:ident  :account/email}]])
  => :accounts

  (determine-rval [[:account :email true]
                   [{:ident  :account/email}]])
  => :email_accounts

  (determine-rval [[:account :image true]
                   [{:ident  :account/bigImage}]])
  => :bigImage_accounts

  (determine-rval [[:node  :node  true]
                   [{:ident  :node/children
                     :ref    {:ns    :node}}]])
  => :children_of

  (determine-rval [[:node  :node  false]
                   [{:ident  :node/children
                     :ref    {:ns    :node
                              :rval  :parents}}]])
  => :parents
  ^:hidden
  ;; (determine-rval [[:account :image true]
  ;;                     [{:ident  :account/big/image}]])
  ;; => :big_image_accounts

  (determine-rval [[:node  :node  false]
                   [{:ident  :node/children
                     :ref    {:ns    :node}}]])
  => :children_of)

^{:refer spirit.common.schema.ref/forward-ref-attr :added "0.3"}
(fact "creates the :ref schema attribute for the forward reference case"

  (forward-ref-attr [{:ident  :node/children
                      :ref    {:ns    :node
                               :rval  :parents}}])
  => [{:ident    :node/children
       :ref      {:ns     :node
                  :type   :forward
                  :val    :children
                  :key    :node/children
                  :rval   :parents
                  :rkey   :node/_children
                  :rident :node/parents}}]

  (forward-ref-attr [{:ident  :node/children
                      :ref    {:ns    :node}}])
  => (throws Exception))

^{:refer spirit.common.schema.ref/reverse-ref-attr :added "0.3"}
(fact "creates the reverse :ref schema attribute for backward reference"

  (reverse-ref-attr [{:ident    :node/children
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

  (reverse-ref-attr [{:ident    :node/children
                      :ref      {:ns     :node}}])
  => (throws Exception))


^{:refer spirit.common.schema.ref/attr-ns-pair :added "0.3"}
(fact "constructs a :ns and :ident root pair for comparison"

  ;; (attr-ns-pair [{:ident  :a/b/c
  ;;                :ref     {:ns :d}}])
  ;; => [:a :d]

  (attr-ns-pair [{:ident  :a/b
                  :ref    {:ns :c}}])
  => [:a :c])

^{:refer spirit.common.schema.ref/mark-multiple :added "0.3"}
(fact "marks multiple ns/ident groups"

  (mark-multiple [[[:a :b] [1 2]]
                  [[:c :d] [1]]])
  => [[[:c :d false] 1]
      [[:a :b true] 1] [[:a :b true] 2]])

^{:refer spirit.common.schema.ref/ref-attrs :added "0.3"}
(fact "creates forward and reverse attributes for a flattened schema"

  (ref-attrs {:account/email [{:ident   :account/email
                               :type    :ref
                               :ref     {:ns  :email}}]})
  => {:email/accounts [{:ident :email/accounts
                        :cardinality :many
                        :type :ref
                        :ref {:type   :reverse
                              :key    :account/_email
                              :ns     :account
                              :val    :accounts
                              :rval   :email
                              :rident :account/email
                              :rkey   :account/email}}]
      :account/email  [{:ident :account/email
                        :type :ref
                        :ref {:type   :forward
                              :key    :account/email
                              :ns     :email
                              :val    :email
                              :rval   :accounts
                              :rident :email/accounts
                              :rkey   :account/_email}}]})
