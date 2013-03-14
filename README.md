# adi

`adi`, rhyming with 'hardy' stands for the acronym (a) (d)atomic (i)nterface. and is an attempt to simplify application and database design, bringing the concept of 'documents' as popularised by mongodb and couchdb to datomic without actually going down that path. Datomic comes with very expressive tools for query using datalog, a lightweight orm type system in the form of the entity function for fast access of related content as well as data transactions and transactional history for the entire lifetime of data as it mutates. adi is datomic for the masses.

However, it is of the author's opinion that the way data has to be imported into datomic does not translate naturally to the way that data is used with an application. In applications, especially ones written in clojure, data are in the form of maps and trees (maps of maps). Whilst working with datomic, the programmer is often forced to manually turn a map-like or tree-like structure into a flat vector-like structure when adding new data or updating existing ones. 

`adi` tries to simplify working with datomic as much as possible by taking a map/tree and generating the vector-like structures using a programmer-specified schema. It can also turn datomic data into 'documents' through a very simple recursive walk. So the programmer have his cake and eat it, leveraging the power of datomic without having to mess with the :db/add and :db/retract syntax (too much).

# The Basics







# Key Concepts

## The Scheme Map

Scheme maps are a more expressive way to write schemas. Essentially, they are a shorthand form of how the data will be added and linked and are used to generate the datomic schema for the application as well as check the integrity of the data being added to datomic database.

We start off by defining a schema:

    (ns adi.intro
      (:require [adi.core :as adi]
                [adi.schema :as as]
                [adi.data :as ad]))

    (def sm-account
      {:account {:username     [{:type        :string}]
                 :password     [{:type        :string}]
                 :permissions  [{:type        :keyword
                                 :cardinality :many}]
                 :points       [{:type        :long
                                 :default     0}]}})

Other options for the scheme-map are: (:unique :doc :index :fulltext :component? :no-history)

We can generate the datomic schema for the scheme map:

    (as/emit-schema sm-account)

    ;;=> ({:db/id {:part :db.part/db, :idx -1000074},
    ;;     :db.install/_attribute :db.part/db,
    ;;     :db/ident :account/password,
    ;;     :db/valueType :db.type/string,
    ;;     :db/cardinality :db.cardinality/one}
    ;;    {:db/id {:part :db.part/db, :idx -1000075},
    ;;     :db.install/_attribute :db.part/db,
    ;;     :db/ident :account/socialMedia,
    ;;     :db/valueType :db.type/ref,
    ;;     :db/cardinality :db.cardinality/many}
    ;;    {:db/id {:part :db.part/db, :idx -1000076},
    ;;     :db.install/_attribute :db.part/db,
    ;;     :db/ident :account/permissions,
    ;;     :db/valueType :db.type/keyword,
    ;;     :db/cardinality :db.cardinality/many}
    ;;    {:db/id {:part :db.part/db, :idx -1000077},
    ;;     :db.install/_attribute :db.part/db,
    ;;     :db/ident :account/username,
    ;;     :db/valueType :db.type/string,
    ;;     :db/cardinality :db.cardinality/one}
    ;;    {:db/id {:part :db.part/db, :idx -1000078},
    ;;     :db.install/_attribute :db.part/db,
    ;;     :db/ident :account/points,
    ;;     :db/valueType :db.type/long,
    ;;     :db/cardinality :db.cardinality/one}
    ;;    {:db/id {:part :db.part/db, :idx -1000079},
    ;;     :db.install/_attribute :db.part/db,
    ;;     :db/ident
    ;;    :account.social/type,
    ;;     :db/valueType :db.type/keyword,
    ;;     :db/cardinality :db.cardinality/one}
    ;;    {:db/id {:part :db.part/db, :idx -1000080},
    ;;     :db.install/_attribute :db.part/db,
    ;;     :db/ident :account.social/name,
    ;;     :db/valueType :db.type/string,
    ;;     :db/cardinality :db.cardinality/one})

The scheme map is much more readable. These will also work as schemas.

    (def sm-account2
      {:account {:password     [{:type :string}],
                 :permissions  [{:cardinality :many, :type :keyword}]}
       :account/username       [{:type :string}],
       :account/points         [{:default 0, :type :long}]})

    (def sm-account3
      {:account/password       [{:type :string}],
       :account/permissions    [{:cardinality :many, :type :keyword}],
       :account/username       [{:type :string}],
       :account/points         [{:default 0, :type :long}]})
    
    (as/emit-schema sm-account2) ;; => same as (as/emit-schema sm-account)
    (as/emit-schema sm-account3) ;; => same as (as/emit-schema sm-account)
 
## The Datastore

Next, we create a datastore, which in reality, is just a map containing a connection object and a schema.

    (def ds (adi/datastore sm-account "datomic:mem://adi-example" true))

    (:fsm ds) ;; => (flatten-all-keys sm-account) 
    (:conn ds) ;; =>  #<LocalConnection datomic.peer.LocalConnection@512772cd>

## Inserting Data

Once a scheme map has been defined, now data can be added:

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

    (apply adi/insert! ds data-account)
    ;;=> .... datomic results ...


At a more primitive level, insert! relys on 'emit' to generating datomic data that goes into datomic:

    (apply ad/emit (flatten-all-keys sm-account) data-account)

    ;;=> ({:db/id {:part :db.part/user, :idx -1000105},
    ;;     :account/password "a123",
    ;;     :account/username "alice",
    ;;     :account/points 0}
    ;;    [:db/add {:part :db.part/user, :idx -1000105}
    ;;     :account/permissions :member]
    ;;    {:db/id {:part :db.part/user, :idx -1000106},
    ;;     :account.social/type :facebook,
    ;;     :account.social/name "bob@facebook.com"}
    ;;    {:db/id {:part :db.part/user, :idx -1000107},
    ;;     :account.social/type :twitter,
    ;;     :account.social/name "bobtwitter"}
    ;;    {:db/id {:part :db.part/user, :idx -1000108},
    ;;     :account/password "b123",
    ;;     :account/username "bob",
    ;;     :account/points 0}
    ;;    [:db/add {:part :db.part/user, :idx -1000108}
    ;;     :account/permissions :admin]
    ;;    [:db/add {:part :db.part/user, :idx -1000108}
    ;;     :account/socialMedia {:part :db.part/user, :idx -1000106}]
    ;;    [:db/add {:part :db.part/user, :idx -1000108}
    ;;     :account/socialMedia {:part :db.part/user, :idx -1000107}]
    ;;    {:db/id {:part :db.part/user, :idx -1000109},
    ;;     :account.social/type :twitter,
    ;;     :account.social/name "charlestwitter"}
    ;;    {:db/id {:part :db.part/user, :idx -1000110},
    ;;     :account.social/type :facebook,
    ;;     :account.social/name "charles@facebook.com"}
    ;;    {:db/id {:part :db.part/user, :idx -1000111},
    ;;     :account/password "b123",
    ;;     :account/username "charles",
    ;;     :account/points 1000}
    ;;    [:db/add {:part :db.part/user, :idx -1000111}
    ;;     :account/permissions :editor]
    ;;    [:db/add {:part :db.part/user, :idx -1000111}
    ;;     :account/permissions :member]
    ;;    [:db/add {:part :db.part/user, :idx -1000111}
    ;;     :account/socialMedia {:part :db.part/user, :idx -1000109}]
    ;;    [:db/add {:part :db.part/user, :idx -1000111}
    ;;     :account/socialMedia {:part :db.part/user, :idx -1000110}]
    ;;    {:db/id {:part :db.part/user, :idx -1000112},
    ;;     :account/password "d123",
    ;;     :account/username "dennis",
    ;;     :account/points 0}
    ;;    [:db/add {:part :db.part/user, :idx -1000112}
    ;;     :account/permissions :member]
    ;;    {:db/id {:part :db.part/user, :idx -1000113},
    ;;     :account/password "e123",
    ;;     :account/username "elaine",
    ;;     :account/points 100}
    ;;    [:db/add {:part :db.part/user, :idx -1000113}
    ;;     :account/permissions :editor]
    ;;    {:db/id {:part :db.part/user, :idx -1000114},
    ;;     :account.social/type :facebook,
    ;;     :account.social/name "fred@facebook.com"}
    ;;    {:db/id {:part :db.part/user, :idx -1000115},
    ;;     :account/password "f123",
    ;;     :account/username "fred",
    ;;     :account/points 5000}
    ;;    [:db/add {:part :db.part/user, :idx -1000115}
    ;;     :account/permissions :editor]
    ;;    [:db/add {:part :db.part/user, :idx -1000115}
    ;;     :account/permissions :admin]
    ;;    [:db/add {:part :db.part/user, :idx -1000115}
    ;;     :account/permissions :member]
    ;;    [:db/add {:part :db.part/user, :idx -1000115}
    ;;     :account/socialMedia {:part :db.part/user, :idx -1000114}])

## Querying Data

Now that there are some data in there, lets do some queries. It seems that having more choice in the way data is queried results in better programs. There are a couple of ways data can be queried:

By Datomic Queries:

    (adi/query ds
      '[:find ?e ?name
        :where
        [?e :account/username ?name]])

    ;; => #<HashSet [[17592186045421 "bob"], [17592186045425 "dennis"], [17592186045428 "fred"], [17592186045418 "alice"], [17592186045426 "elaine"], [17592186045424 "charles"]]>

By Id:

    (adi/select ds 17592186045421)

    ;;=> ({:db/id 17592186045421
           :account {:points 0,
    ;;               :username "bob",
    ;;               :permissions #{:admin},
    ;;               :socialMedia #{{:+ {:db/id 17592186045420}} 
    ;;                              {:+ {:db/id 17592186045419}}},
    ;;               :password "b123"}})

By Hashmap:

    (adi/select ds {:account/permissions :editor})

    ;;=> ({:db/id 17592186045424
    ;;     :account {:points 1000,
    ;;               :username "charles",
    ;;               :permissions #{:editor :member},
    ;;               :socialMedia #{{:+ {:db/id 17592186045422}} 
    ;;                             {:+ {:db/id 17592186045423}}},
    ;;               :password "b123"}}
    ;;     {:db/id 17592186045426
    ;;      :account {:points 100,
    ;;                :username "elaine",
    ;;                :permissions #{:editor},
    ;;                :password "e123"}}
    ;;    
    ;;     {:db/id 17592186045428
    ;;      :account {:points 5000,
    ;;                :username "fred",
    ;;                :permissions #{:editor :admin :member},
    ;;                :socialMedia #{{:+ {:db/id 17592186045427}}},
    ;;                :password "f123"}})

By Combination:



## History

Historical data contains the the real insight that analysts are after

FIXME

## License

Copyright © 2013 Chris Zheng

Distributed under the Eclipse Public License, the same as Clojure.
