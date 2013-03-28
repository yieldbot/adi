(ns adi.test-schema
  (:use midje.sweet
        adi.utils
        adi.schema
        adi.checkers))

(fact "infer-idents"
  (infer-idents {:a [{}]})
  => {:a [{:ident :a}]}

  (infer-idents {:a [{}]
               :b [{}]})
  => {:a [{:ident :a}]
      :b [{:ident :b}]}

  (infer-idents {:a/b [{}]
               :a/c [{}]})
  => {:a {:b [{:ident :a/b}]
          :c [{:ident :a/c}]}}

  (infer-idents {:a {:b [{}]
                   :c [{}]}})
  => {:a {:b [{:ident :a/b}]
          :c [{:ident :a/c}]}})


(def rf1-geni
  (infer-idents {:a    [{:type   :ref}]
                 :b    [{:type   :long}]
                 :c    [{:type   :ref
                         :ref-ns :x}]
                 :d    {:e  [{:type   :ref}]
                        :f  [{:type   :ref
                              :ref-ns :y}]
                        :_g  [{:type   :ref
                               :ref-ns :y}]}}))

(fact "find-ref-idents and find-reversible-ref-idents"
  (keys (find-ref-idents (flatten-keys-in rf1-geni)))
  => (contains [:a :c :d/e :d/f :d/_g] :in-any-order)

  (keys (find-reversible-ref-idents (flatten-keys-in rf1-geni)))
  => (just [:d/f]))


(fact "vec-reversible-lu"
  (vec-reversible-lu [{:ident  :a/b/c
                       :ref-ns :d/e}])
  => [:a :d/e])

(fact "make-rev-meta-ident"
  (make-rev-meta-ident [[:account :email false]
                        [{:ident  :account/email}]])
  => :email/_accounts

  (make-rev-meta-ident [[:account :email true]
                        [{:ident  :account/email}]])
  => :email/_email_accounts

  (make-rev-meta-ident [[:account :image true]
                        [{:ident  :account/bigImage}]])
  => :image/_bigImage_accounts

  (make-rev-meta-ident [[:account :image true]
                        [{:ident  :account/big/image}]])
  => :image/_big_image_accounts

  (make-rev-meta-ident [[:node :node true]
                        [{:ident  :node/parent}]])
  => :node/_parent_referrers

  (make-rev-meta-ident [[:node :node true]
                        [{:ident  :node/child/node}]])
  => :node/_child_node_referrers)

(fact "make-rev-meta"
  (make-rev-meta [[:account :email false]
                      [{:ident  :account/email}]])
  => [{:type        :ref
       :ref-key     :account/_email
       :ref-ns      :account
       :cardinality :many
       :ident       :email/_accounts}])

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
  (remove-rev-idents (infer-rev-refs s5-geni))
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
  (remove-rev-idents (infer-rev-refs s6-geni))
  => s6-geni)

(fact "rev-ident?"
  (rev-ident? :a/_b) => true
  (rev-ident? :a/b) => false
  (rev-ident? :_b) => true)

(fact "find-rev-idents"
  (keys (find-rev-idents {:_a  [{:type        :ref
                                 :ref-key     :account/_bigImage}]
                          :_b  [{:type        :ref}]}))
  => (just [:_a]))


(fact "meta-property"
  (meta-property :string :type) => :db.type/string
  (meta-property :long :type) => :db.type/long
  (meta-property :one :cardinality) => :db.cardinality/one
  (meta-property :value :unique) => :db.unique/value)


(fact "geni-property"
  (geni-property {:type :string} :type (meta-geni :type) {})
  => {:db/valueType :db.type/string}

  (geni-property {:cardinality :one} :cardinality (meta-geni :cardinality) {})
  => {:db/cardinality :db.cardinality/one}

  (geni-property {} :cardinality (meta-geni :cardinality) {})
  => {:db/cardinality :db.cardinality/one}

  (geni-property {} :unique (meta-geni :unique) {})
  => {}

  (geni-property {} :type (meta-geni :type) {})
  => (throws Exception)

  (geni-property {:type :ERROR} :type (meta-geni :type) {})
  => (throws Exception))

(fact "build-single-schema"
  (build-single-schema {:ident :name
                        :type  :string})
  => (contains {:db.install/_attribute :db.part/db,
                :db/ident :name,
                :db/valueType :db.type/string,
                :db/cardinality :db.cardinality/one})

  (build-single-schema {:ident       :account/tags
                        :type        :string
                        :cardinality :many
                        :fulltext    true
                        :index       true
                        :doc         "tags for account"})
  => (contains {:db.install/_attribute :db.part/db
                :db/ident        :account/tags
                :db/index        true
                :db/doc          "tags for account"
                :db/valueType    :db.type/string
                :db/fulltext     true
                :db/cardinality  :db.cardinality/many}))


(fact "build-schema"
  (map #(dissoc % :db/id) (build-schema (infer-all s6-geni)))
  => '({:db.install/_attribute :db.part/db
        :db/ident              :node/male/node
        :db/valueType          :db.type/ref
        :db/cardinality        :db.cardinality/one}
       {:db.install/_attribute :db.part/db
        :db/ident              :node/female/node
        :db/valueType          :db.type/ref
        :db/cardinality        :db.cardinality/one}))


(fact "find-keys"
  (find-keys {:name [{:type :string}]} :type :string)
  => #{:name}

  (find-keys {:name [{:type :string}]} :type :other)
  => #{}

  (find-keys (flatten-keys-in (infer-all s1-geni))
             :cardinality :many)
  => #{:email/_accounts}

  (find-keys (flatten-keys-in (infer-all s1-geni)) #{:email}
             :cardinality :many)
  => #{:email/_accounts}

  (find-keys (flatten-keys-in (infer-all s1-geni)) #{:account}
             :cardinality :many)
  => #{}

  (find-keys (flatten-keys-in (infer-all s6-geni))
             :cardinality :many)
  => #{:node/_female_node_referrers :node/_male_node_referrers})
