(ns adi.test-schema
  (:use midje.sweet
        adi.schema
        hara.common
        hara.hash-map))


(fact "meta-property"
  (meta-property :string :type) => :db.type/string
  (meta-property :long :type) => :db.type/long
  (meta-property :one :cardinality) => :db.cardinality/one
  (meta-property :value :unique) => :db.unique/value)

(fact "add-ident"
  (add-ident [:a [{}]])
  => [:a [{:ident :a}]])

(fact "infer-idents"
  (infer-idents {:a [{}]})
  => {:a [{:ident :a}]}

  (infer-idents {:a [{}]
                 :b [{}]})
  => {:a [{:ident :a}]
      :b [{:ident :b}]}

  (infer-idents {:a/b [{}]
                 :a/c [{}]})
  => {:a/b [{:ident :a/b}]
      :a/c [{:ident :a/c}]})

(fact "add-defaults"
  (add-defaults [:a [{}]] [])
  => [:a [{}]]

  (add-defaults [:a [{}]] meta-geni-add-defaults)
  => [:a [{:cardinality :one :type :string}]]

  (add-defaults [:a [{}]] meta-geni-all-defaults)
  => [:a [{:index false, :fulltext false,
           :cardinality :one, :noHistory false
           :type :string}]])

(fact "infer-defaults"
  (infer-defaults {:a [{}]})
  => {:a [{:cardinality :one :type :string}]}

  (infer-defaults {:a [{:cardinality :many}]})
  => {:a [{:cardinality :many :type :string}]}

  (infer-defaults {:a [{:ident :a
                        :type  :string
                        :index true
                        :fulltext true
                        :cardinality :many
                        :noHistory true}]} meta-geni-all-defaults)
  => {:a [{:ident :a
           :type  :string
           :index true
           :fulltext true
           :cardinality :many
           :noHistory true}]})

(fact "flip-ident"
  (flip-ident :b) => :_b
  (flip-ident :_b) => :b
  (flip-ident :a/b) => :a/_b
  (flip-ident :a/_b) => :a/b
  (flip-ident :a/b/c) => :a/b/_c
  (flip-ident :a/b/_c) => :a/b/c)

(def rr1 {:ident   :account/email
          :type    :ref
          :ref     {:ns  :email}})

(fact "reversible-ref?"
  (reversible-ref? [rr1])
  => true

  (reversible-ref? [(merge rr1 {:ident :email})])
  => falsey

  (reversible-ref? [(merge rr1 {:ident :accout/_email})])
  => falsey

  (reversible-ref? [(merge rr1 {:type :long})])
  => falsey

  (reversible-ref? [(merge-nested rr1 {:ref {:norev true}})])
  => falsey

  (reversible-ref? [(dissoc-in-keep rr1 [:ref :ns])])
  => falsey)

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

(fact "find-reversible-ref-idents"
  (find-reversible-ref-idents
   {:account/email [{:ident   :account/email
                     :type    :ref
                     :ref     {:ns  :email}}]})
  => [:account/email])

(fact "vec-reversible-lu"
  (vec-reversible-lu [{:ident  :a/b/c
                       :ref    {:ns :d}}])
  => [:a :d])

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


  (determine-ref-rval [[:node  :node  false]
                   [{:ident  :node/children
                     :ref    {:ns    :node}}]])
  => :children_referrers

  (determine-ref-rval [[:node  :node  false]
                   [{:ident  :node/children
                     :ref    {:ns    :node
                              :rval  :parents}}]])
  => :parents)

(fact "attach-ref-rval"
  (attach-ref-rval [[:node  :node  false]
                   [{:ident  :node/children
                     :ref    {:ns    :node}}]])
  => [{:ident :node/children
       :ref   {:ns   :node
               :rval :children_referrers}}]

  (attach-ref-rval [[:node  :node  false]
                   [{:ident  :node/children
                     :ref    {:ns    :node
                              :rval  :parents}}]])
  => [{:ident :node/children
       :ref   {:ns   :node
               :rval :parents}}])

(fact "determine-ref-meta"
  (determine-ref-meta [{:ident  :node/children
                        :ref    {:ns    :node}}])
  => (throws Exception)


  (determine-ref-meta [{:ident  :node/children
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

(fact "attach-ref-meta"
  (attach-ref-meta [[:node  :node  false]
                    [{:ident  :node/children
                      :ref    {:ns    :node}}]])
  => [{:ident :node/children
       :ref   {:ns     :node
               :type   :forward
               :val    :children
               :key    :node/children
               :rval   :children_referrers
               :rkey   :node/_children
               :rident :node/children_referrers}}])

(fact "determine-revref-meta"
  (determine-revref-meta [{:ident    :node/children
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

  (determine-revref-meta [{:ident    :node/children
                           :ref      {:ns     :node}}])
  => (throws Exception))

(fact "gather-reversible-refs"
  (gather-reversible-refs {:account/email [{:ident   :account/email
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

(fact "infer-refs"
  (infer-refs {:account/email [{:ident   :account/email
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
       :account/email [{:ref {:rident :email/accounts
                               :rkey :account/_email
                               :val :email
                               :key :account/email
                               :type :forward
                               :rval :accounts
                               :ns :email}
                         :type :ref
                         :ident :account/email}]})

(fact "find-revrefs-idents"
  (find-revref-idents (infer-refs {:account/email [{:ident   :account/email
                                                    :type    :ref
                                                    :ref     {:ns  :email}}]}))
  => '(:email/accounts))

(fact "remove-revref-idents"
  (remove-revrefs
   (infer-refs {:account/email [{:ident   :account/email
                                        :type    :ref
                                        :ref     {:ns  :email}}]}))
  => (just {:account/email vector?}))

(fact "make-scheme-model"
  (make-scheme-model {:account/email [{:ident   :account/email
                                       :type    :ref
                                       :ref     {:ns  :email}}]})
  => (contains
      {:lu {:account/email :account/email
            :email/accounts :email/accounts
            :account/_email :email/accounts}}))

(fact "remove-enums"
  (remove-enums {:person/gender [{:ident   :person/gender
                                     :type    :enum
                                     :enum    {:ns     :person.gender
                                               :values #{:male  :female}}}]})
  => {:person/gender   [{:ident :person/gender
                         :type  :ref
                         :ref   {:ns     :person.gender
                                 :values #{:male  :female}
                                 :type   :enum-rel}}]})


(fact "emit-schema-property"
  (emit-schema-property {:type :string} :type (meta-geni :type) {})
  => {:db/valueType :db.type/string}

  (emit-schema-property {:cardinality :one} :cardinality (meta-geni :cardinality) {})
  => {:db/cardinality :db.cardinality/one}

  (emit-schema-property {} :cardinality (meta-geni :cardinality) {})
  => {:db/cardinality :db.cardinality/one}

  (emit-schema-property {} :unique (meta-geni :unique) {})
  => {}

  (emit-schema-property {} :type (meta-geni :type) {})
  => {:db/valueType :db.type/string}

  (emit-schema-property {:type :ERROR} :type (meta-geni :type) {})
  => (throws Exception))


(fact "emit-single-schema"
  (emit-single-schema [{:ident :name
                         :type  :string}])
    => (contains {:db.install/_attribute :db.part/db,
                  :db/ident :name,
                  :db/valueType :db.type/string,
                  :db/cardinality :db.cardinality/one})

    (emit-single-schema [{:ident       :account/tags
                          :type        :string
                          :cardinality :many
                          :fulltext    true
                          :index       true
                          :doc         "tags for account"}])
    => (contains {:db.install/_attribute :db.part/db
                  :db/ident        :account/tags
                  :db/index        true
                  :db/doc          "tags for account"
                  :db/valueType    :db.type/string
                  :db/fulltext     true
                  :db/cardinality  :db.cardinality/many}))


  (fact "emit-schema"
    (emit-schema (infer-fgeni {:node/male/node   [{:type  :ref :ref-ns  :node}]
                               :node/female/node [{:type  :ref :ref-ns  :node}]}))
    => (just
        [(contains {:db/ident              :node/male/node
                    :db/valueType          :db.type/ref
                    :db/cardinality        :db.cardinality/one})
         (contains {:db/ident              :node/female/node
                    :db/valueType          :db.type/ref
                    :db/cardinality        :db.cardinality/one})])


    (emit-schema  {:person/gender [{:ident   :person/gender
                                    :type    :enum
                                    :enum    {:ns     :person.gender
                                              :values #{:male  :female}}}]})
    => (just [(contains {:db/ident       :person/gender
                         :db/cardinality :db.cardinality/one
                         :db/valueType   :db.type/ref})
              (contains {:db/ident       :person.gender/male})
              (contains {:db/ident       :person.gender/female})] :in-any-order))


(fact "find-keys"
    (find-keys {:name [{:type :string}]} :type :string)
    => #{:name}

    (find-keys {:name [{:type :string}]} :type :other)
    => #{}

    (find-keys {:name [{:type :ref
                        :ref {:type :forward}}]}
               :ref (fn [r] (= :forward (:type r))))
    => #{:name}

    (find-keys {:name [{:type :ref
                        :ref {:type :forward}}]}
               (constantly true)
               :type :ref :ref (fn [r] (= :forward (:type r))))
    => #{:name})
















(comment
  (def s1-geni
    (infer-idents {:account {:email [{:type    :ref
                                      :ref-ns  :email}]}}))

  (fact "s1-geni"
    (infer-rev-refs s1-geni)
    => {:account {:email     [{:type        :ref
                               :ref-ns      :email
                               :ident       :account/email}]}
        :email   {:_accounts [{:type        :ref
                               :ref-key     :account/_email
                               :ref-ns      :account
                               :cardinality :many
                               :ident       :email/_accounts}]}})

  (def s2-geni
    (infer-idents {:account {:bigImage    [{:type    :ref
                                            :ref-ns  :image}]
                             :smallImage  [{:type    :ref
                                            :ref-ns  :image}]}}))

  (fact "s2-geni"
    (infer-rev-refs s2-geni)
    => {:account  {:bigImage      [{:type      :ref
                                    :ref-ns    :image
                                    :ident     :account/bigImage}]
                   :smallImage    [{:type      :ref
                                    :ref-ns    :image
                                    :ident     :account/smallImage}]}
        :image    {:_bigImage_accounts    [{:type        :ref
                                            :ref-key     :account/_bigImage
                                            :ref-ns      :account
                                            :cardinality :many
                                            :ident       :image/_bigImage_accounts}]
                   :_smallImage_accounts  [{:type        :ref
                                            :ref-key     :account/_smallImage
                                            :ref-ns      :account
                                            :cardinality :many
                                            :ident       :image/_smallImage_accounts}]}})


  (def s3-geni
    (infer-idents {:account {:profile/image    [{:type  :ref :ref-ns  :image}]
                             :main/image       [{:type  :ref :ref-ns  :image}]}}))
  (fact "s3-geni"
    (infer-rev-refs s3-geni)
    => {:account  {:profile    {:image  [{:type      :ref
                                          :ref-ns    :image
                                          :ident     :account/profile/image}]}
                   :main       {:image  [{:type      :ref
                                          :ref-ns    :image
                                          :ident     :account/main/image}]}}
        :image    {:_profile_image_accounts [{:type        :ref
                                              :ref-key     :account/profile/_image
                                              :ref-ns      :account
                                              :cardinality :many
                                              :ident       :image/_profile_image_accounts}]
                   :_main_image_accounts    [{:type        :ref
                                              :ref-key     :account/main/_image
                                              :ref-ns      :account
                                              :cardinality :many
                                              :ident       :image/_main_image_accounts}]}})


  (def s4-geni
    (infer-idents {:node {:children  [{:type  :ref :ref-ns  :node}]
                          :parent    [{:type  :ref :ref-ns  :node}]}}))

  (fact "s4-geni"
    (infer-rev-refs s4-geni)
    => {:node {:children             [{:type        :ref
                                       :ref-ns      :node
                                       :ident       :node/children}]
               :_children_referrers  [{:type        :ref
                                       :ref-key     :node/_children
                                       :ref-ns      :node
                                       :cardinality :many
                                       :ident       :node/_children_referrers}]
               :parent               [{:type        :ref
                                       :ref-ns      :node
                                       :ident       :node/parent}]
               :_parent_referrers    [{:type        :ref
                                       :ref-key     :node/_parent
                                       :ref-ns      :node
                                       :cardinality :many
                                       :ident       :node/_parent_referrers}]}})


  (def s5-geni
    (infer-idents {:node {:male   {:parent  [{:type  :ref :ref-ns  :node}]}
                          :female {:parent  [{:type  :ref :ref-ns  :node}]}}}))

  (fact "s5-geni"
    (infer-rev-refs s5-geni)
    => {:node {:male    {:parent [{:ref-ns :node, :type :ref, :ident :node/male/parent}]}
               :female  {:parent [{:ref-ns :node, :type :ref, :ident :node/female/parent}]}
               :_male_parent_referrers   [{:type        :ref
                                           :ref-key     :node/male/_parent
                                           :ref-ns      :node
                                           :cardinality :many
                                           :ident       :node/_male_parent_referrers}]
               :_female_parent_referrers [{:type        :ref
                                           :ref-key     :node/female/_parent
                                           :ref-ns      :node
                                           :cardinality :many
                                           :ident :node/_female_parent_referrers}]}}
    (remove-flip-idents (infer-rev-refs s5-geni))
    => s5-geni)

  (def s6-geni
    (infer-idents {:node {:male   {:node  [{:type  :ref :ref-ns  :node}]}
                          :female {:node  [{:type  :ref :ref-ns  :node}]}}}))

  (fact "s6-geni"
    (infer-rev-refs s6-geni)
    => {:node {:male   {:node [{:ref-ns :node, :type :ref, :ident :node/male/node}]}
               :female {:node [{:ref-ns :node, :type :ref, :ident :node/female/node}]}
               :_male_node_referrers   [{:type        :ref
                                         :ref-key     :node/male/_node
                                         :ref-ns      :node
                                         :cardinality :many
                                         :ident       :node/_male_node_referrers}]
               :_female_node_referrers [{:type        :ref
                                         :ref-key     :node/female/_node
                                         :ref-ns      :node
                                         :cardinality :many
                                         :ident       :node/_female_node_referrers}]}}
    (remove-flip-idents (infer-rev-refs s6-geni))
    => s6-geni)

  (fact "find-flip-idents"
    (keys (find-flip-idents {:_a  [{:type        :ref
                                    :ref-key     :account/_bigImage}]
                             :_b  [{:type        :ref}]}))
    => (just [:_a]))






  (fact "find-keys"
    (find-keys {:name [{:type :string}]} :type :string)
    => #{:name}

    (find-keys {:name [{:type :string}]} :type :other)
    => #{}

    (find-keys (flatten-keys-nested (infer-all s1-geni))
               :cardinality :many)
    => #{:email/_accounts}

    (find-keys (flatten-keys-nested (infer-all s1-geni)) #{:email}
               :cardinality :many)
    => #{:email/_accounts}

    (find-keys (flatten-keys-nested (infer-all s1-geni)) #{:account}
               :cardinality :many)
    => #{}

    (find-keys (flatten-keys-nested (infer-all s6-geni))
               :cardinality :many)
    => #{:node/_female_node_referrers :node/_male_node_referrers}))
