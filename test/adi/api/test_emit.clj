(ns adi.api.test-emit
 (:use midje.sweet
       adi.utils
       adi.data
       adi.schema
       adi.api
       adi.checkers))

(fact "emit-schema takes a scheme-map and turns it into a schema
       that is installable into datomic"

  (emit-schema {"name" [{:type :ref}]})
  => (exclude-ids [{:db.install/_attribute :db.part/db,
                    :db/ident :name,
                    :db/valueType :db.type/ref,
                    :db/cardinality :db.cardinality/one}])

  (emit-schema
   {:link
    {:next  [{:type :ref :ref-ns :link}]
     :value [{:type :string :default "undefined"}]}})
  => (exclude-ids [{:db.install/_attribute :db.part/db,
                    :db/ident :link/next,
                    :db/valueType :db.type/ref,
                    :db/cardinality :db.cardinality/one}
                   {:db.install/_attribute :db.part/db,
                    :db/ident :link/value,
                    :db/valueType :db.type/string,
                    :db/cardinality :db.cardinality/one}])

  (emit-schema
   {:link/next  [{:type :ref :ref-ns :link}]}
   {:link/value [{:type :string :default "undefined"}]})
  => (exclude-ids [{:db.install/_attribute :db.part/db,
                    :db/ident :link/next,
                    :db/valueType :db.type/ref,
                    :db/cardinality :db.cardinality/one}
                   {:db.install/_attribute :db.part/db,
                    :db/ident :link/value,
                    :db/valueType :db.type/string,
                    :db/cardinality :db.cardinality/one}]))

(fact "emit schema full options"
  (emit-schema {:name [{:type :string
                        :cardinality :many
                        :unique :value
                        :doc "The name of something"
                        :index true
                        :fulltext true
                        :no-history true
                        :OTHERS :WILL-NOT-SHOW}]})
  => (exclude-ids [{:db.install/_attribute :db.part/db,
                    :db/ident :name
                    :db/valueType :db.type/string
                    :db/cardinality :db.cardinality/many
                    :db/unique :db.unique/value
                    :db/doc "The name of something"
                    :db/index true
                    :db/fulltext true
                    :db/no-history true}]))

(fact "more usages"
  (emit-schema
   {:account
    {:username  [{:type        :string
                  :unique      :value
                  :doc         "The username associated with the account"}]
     :password  [{:type        :string
                  :doc         "The password associated with the account"}]}})
  => (exclude-ids [{:db.install/_attribute :db.part/db,
                    :db/ident :account/password,
                    :db/doc "The password associated with the account",
                    :db/valueType :db.type/string,
                    :db/cardinality :db.cardinality/one}
                   {:db.install/_attribute :db.part/db,
                    :db/ident :account/username,
                    :db/doc "The username associated with the account",
                    :db/valueType :db.type/string,
                    :db/unique :db.unique/value,
                    :db/cardinality :db.cardinality/one}]))


(fact "emit-schema exceptions. All these values should blow up:"
  (emit-schema {:name [{}]})
  => (throws Exception)

  (emit-schema {:name [{:type :wrong-type}]})
  => (throws Exception)

  (emit-schema {:name [{:type :ref
                        :doc :NOT-STRING}]})
  => (throws Exception)

  (emit-schema {:name [{:type :ref
                        :cardinality :not-one-or-many}]})
  => (throws Exception)

  (emit-schema {:name [{:type :ref
                        :unique :not-value-or-identity}]})
  => (throws Exception)

  (emit-schema {:name [{:type :ref
                        :index :not-a-bool-value}]})
  => (throws Exception))


(def s1-geni
  (add-idents
   {:account
    {:username    [{:type        :string
                    :required    true}]
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

(def s1-opts
  {:geni s1-geni
   :fgeni (flatten-all-keys s1-geni)})

(fact "generating a massive schema"
  (emit-schema s1-geni)
  => (exclude-ids
      '({:db.install/_attribute :db.part/db,
         :db/ident :account/hash,
         :db/valueType :db.type/string,
         :db/cardinality :db.cardinality/one}
        {:db.install/_attribute :db.part/db,
         :db/ident :account/firstName,
         :db/valueType :db.type/string,
         :db/cardinality :db.cardinality/one}
        {:db.install/_attribute :db.part/db,
         :db/ident :account.address/country,
         :db/valueType :db.type/string,
         :db/cardinality :db.cardinality/one}
        {:db.install/_attribute :db.part/db,
         :db/ident :account/contacts,
         :db/valueType :db.type/ref,
         :db/cardinality :db.cardinality/many}
        {:db.install/_attribute :db.part/db,
         :db/ident :account/business/industry,
         :db/valueType :db.type/string,
         :db/cardinality :db.cardinality/many}
        {:db.install/_attribute :db.part/db,
         :db/ident :account.contact/field,
         :db/valueType :db.type/string,
         :db/cardinality :db.cardinality/one}
        {:db.install/_attribute :db.part/db,
         :db/ident :account/username,
         :db/valueType :db.type/string,
         :db/cardinality :db.cardinality/one}
        {:db.install/_attribute :db.part/db,
         :db/ident :account.address/city,
         :db/valueType :db.type/string,
         :db/cardinality :db.cardinality/one}
        {:db.install/_attribute :db.part/db,
         :db/ident :account/business/desc,
         :db/valueType :db.type/string,
         :db/cardinality :db.cardinality/one}
        {:db.install/_attribute :db.part/db,
         :db/ident :account/isActivated,
         :db/valueType :db.type/boolean,
         :db/cardinality :db.cardinality/one}
        {:db.install/_attribute :db.part/db,
         :db/ident :account/email,
         :db/valueType :db.type/ref,
         :db/cardinality :db.cardinality/one}
        {:db.install/_attribute :db.part/db,
         :db/ident :account/joined,
         :db/valueType :db.type/instant,
         :db/cardinality :db.cardinality/one}
        {:db.install/_attribute :db.part/db,
         :db/ident :account/address/all,
         :db/valueType :db.type/ref,
         :db/cardinality :db.cardinality/many}
        {:db.install/_attribute :db.part/db,
         :db/ident :account/address/billing,
         :db/valueType :db.type/ref,
         :db/cardinality :db.cardinality/one}
        {:db.install/_attribute :db.part/db,
         :db/ident :account/business/name,
         :db/valueType :db.type/string,
         :db/cardinality :db.cardinality/one}
        {:db.install/_attribute :db.part/db,
         :db/ident :account/lastName,
         :db/valueType :db.type/string,
         :db/cardinality :db.cardinality/one}
        {:db.install/_attribute :db.part/db,
         :db/ident :account/address/shipping,
         :db/valueType :db.type/ref,
         :db/cardinality :db.cardinality/one}
        {:db.install/_attribute :db.part/db,
         :db/ident :account/isVerified,
         :db/valueType :db.type/boolean,
         :db/cardinality :db.cardinality/one}
        {:db.install/_attribute :db.part/db,
         :db/ident :account/business/abn,
         :db/valueType :db.type/string,
         :db/cardinality :db.cardinality/one}
        {:db.install/_attribute :db.part/db,
         :db/ident :account.contact/type,
         :db/valueType :db.type/keyword,
         :db/cardinality :db.cardinality/one}
        {:db.install/_attribute :db.part/db,
         :db/ident :account.address/region,
         :db/valueType :db.type/string,
         :db/cardinality :db.cardinality/one}
        {:db.install/_attribute :db.part/db,
         :db/ident :account.address/postcode,
         :db/valueType :db.type/string,
         :db/cardinality :db.cardinality/one}
        {:db.install/_attribute :db.part/db,
         :db/ident :account.address/line2,
         :db/valueType :db.type/string,
         :db/cardinality :db.cardinality/one}
        {:db.install/_attribute :db.part/db,
         :db/ident :account.address/line1,
         :db/valueType :db.type/string,
         :db/cardinality :db.cardinality/one})))


(fact "emit-ref-set"
  (emit-ref-set (flatten-all-keys s1-geni){})
  => #{:account/contacts :account/email :account/address/all
       :account/address/billing :account/address/shipping}
  (emit-ref-set (flatten-all-keys s1-geni){:ns-set #{:account}})
  => #{:account/contacts :account/email :account/address/all
       :account/address/billing :account/address/shipping}
  (emit-ref-set (flatten-all-keys s1-geni){:ns-set #{}})
  => #{})


;;;; emit-insert

(fact "emit insert"
  (emit-insert {:account {:username "chris"}}
               (assoc s1-opts :defaults? false))
  => (exclude-ids [{:account/username "chris"}])

  (emit-insert {:account {:username "chris"}}
               s1-opts)

  => (exclude-ids [{:account/username "chris"
                    :account/isActivated false,
                    :account/isVerified false}])

  (emit-insert {:db/id 1
                :account {:username "chris"
                          :email {:+/db/id 2
                                    :field "z@caudate.me"}}}
               s1-opts)
  => '({:db/id 2, :account.contact/field "z@caudate.me"}
       {:db/id 1,
        :account/username "chris",
        :account/isActivated false,
        :account/isVerified false}
       [:db/add 1 :account/email 2])


  (emit-insert {:db/id 1
                :account/username "chris"
                :account/email/+/db/id 2
                :account/email/field "z@caudate.me"}
               s1-opts)

  => '({:db/id 2, :account.contact/field "z@caudate.me"}
       {:db/id 1,
        :account/username "chris",
        :account/isActivated false,
        :account/isVerified false}
       [:db/add 1 :account/email 2])

  (emit-insert {:db/id 1
                :account {:email {:+/db/id 2
                                    :field "z@caudate.me"}}}
               s1-opts)
  => (throws Exception)

  (emit-insert {:db/id 1
                :account {:name "chris"
                          :OTHER :NOT-NEEDED
                          :email {:+/db/id 2
                                    :field "z@caudate.me"}}}
               s1-opts)
  => (throws Exception))



(fact "emit update"
  (emit-update {:db/id 1
                :account {:username "chris"}}
               s1-opts)
  => [{:db/id 1 :account/username "chris"}]

  (emit-update {:db/id 1
                :account {:username "chris"
                          :email {:+/db/id 2
                                    :field "z@caudate.me"}}}
                 s1-opts)
  => '({:db/id 2, :account.contact/field "z@caudate.me"}
       {:db/id 1,
        :account/username "chris"}
       [:db/add 1 :account/email 2])


  (emit-update {:db/id 1
                :account/username "chris"
                :account/email/+/db/id 2
                :account/email/field "z@caudate.me"}
               s1-opts)
  => '({:db/id 2, :account.contact/field "z@caudate.me"}
       {:db/id 1,
        :account/username "chris"}
        [:db/add 1 :account/email 2])

  (emit-update {:db/id 1
                :account {:email {:+/db/id 2
                                    :field "z@caudate.me"}}}
               s1-opts)
  => '({:account.contact/field "z@caudate.me", :db/id 2}
       [:db/add 1 :account/email 2])

  (emit-update {:db/id 1
                :account {:username "chris"
                          :OTHER :NOT-NEEDED
                          :email {:+/db/id 2
                                  :field "z@caudate.me"}}}
               s1-opts)
  => '({:db/id 2, :account.contact/field "z@caudate.me"}
       {:db/id 1,
        :account/username "chris"}
        [:db/add 1 :account/email 2]))


(fact "emit query"
  (emit-query {:#/sym '?e
               :account {:username "chris"}}
              s1-opts)
  => '[:find ?e :where [?e :account/username "chris"]]

  (emit-query {:#/sym '?e
               :account {:username "chris"
                         :hash "hello"}}
              s1-opts)
  => '[:find ?e :where
       [?e :account/username "chris"]
       [?e :account/hash "hello"]]

   (emit-query {:#/sym '?e
                :account {:username '_
                         :hash "hello"}}
              s1-opts)
   => '[:find ?e :where
        [?e :account/username _]
        [?e :account/hash "hello"]]

  (emit-query {:#/sym '?e
               :account {:username "chris"}
               :#/q [['?e :account/hash "hello"]]}
              s1-opts)
  => '[:find ?e :where
       [?e :account/username "chris"]
       [?e :account/hash "hello"]]

  (emit-query {:#/sym '?e
               :account {:email {:#/sym '?m
                                 :field "z@caudate.me"}}}
              s1-opts)
  => '[:find ?e :where
       [?e :account/email ?m]
       [?m :account.contact/field "z@caudate.me"]]

  (clauses
   (-> {:#/sym '?e
        :#/fulltext {:account/username "chris"}}
       (process (:geni s1-opts) {:sets-only? true})
       (characterise (:fgeni s1-opts) {:generate-syms true}))
   (assoc s1-opts :pretty-gen true))
  => '[:find ?e :where
       [(fulltext $ :account/username "chris") [[?e ?ft1]]]]

  (emit-query {:#/sym '?e
               :#/fulltext {:account/username "chris"}}
              (assoc s1-opts :pretty-gen true))
  => '[:find ?e :where
       [(fulltext $ :account/username "chris") [[?e ?ft1]]]]
)
