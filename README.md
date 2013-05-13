# adi

`adi`, rhyming with 'hardy' stands for the acronym (a) (d)atomic (i)nterface. 

The concept is simple. `adi` is a Document Database onto grafted on Datomic. It makes use of a map/object notation to interface with a Logical Query Engine.

Datomic began something brand new for data, and `adi` leverages that incredible flexiblility with a syntax that is simple to understand.  It converts flat, record-like arrays to tree-like objects and back again so that the user can interface with datomic the way datomic was designed to do.

The key to understanding `adi` lies in understanding the power of a schema. The schema dictates what you can do with the data. Instead of limiting the programmer, the schema should exhance him/her. 

Using `adi` once a schema for an application has been defined, the data can be inserted in any shape, as long as it follows the coventions specified within that schema. Fundamentally, there should be no difference in the data-structure between what the programmer asks for and what the programmer is getting. We shouldn't have to play around turning objects into records, objects into logic queries... etc... 

Well... Not anymore...

#### Installation

In your project file, add

```clojure
[adi "0.1.1"]
```

### The Overview

This is a longish tutorial, mainly because of the data we have to import:

We want to model a simple school, and we have the standard information like classes, teachers students.

```clojure
(ns example.school
  (:use [adi.utils :only [iid ?q]])
  (:require [adi.core :as adi]))

(def class-schema
  {:class   {:type    [{:type :keyword}]
             :name    [{:type :string}]
             :accelerated [{:type :boolean}]
             :teacher [{:type :ref                  ;; <- Note that refs allow a reverse
                        :ref  {:ns   :teacher       ;; look-up to be defined to allow for more
                               :rval :teaches}}]}   ;; natural expression. In this case,
   :teacher {:name     [{:type :string}]            ;; we say that every `class` has a `teacher`
             :canTeach [{:type :keyword             ;; so the reverse will be defined as a
                         :cardinality :many}]       ;; a `teacher` `teaches` a class
             :pets     [{:type :keyword
                         :cardinality :many}]}
   :student {:name     [{:type :string}]
             :siblings [{:type :long}]
             :classes    [{:type :ref
                         :ref   {:ns   :class
                                 :rval :students}
                         :cardinality :many}]}})
```

Here, we create a `datastore`, which is a thin wrapper around the datomic connection object

```clojure
(def class-datastore
  (adi/datastore "datomic:mem://class-datastore" class-schema true true))
```

Now is the fun part: Lets fill in the data. This is one way of filling out the data. Note that it is object-like in nature, with links defined through ids. If it doesn't contain an id, the record is automatically created:

```clojure
(def class-data
  [{:db/id (iid :EnglishA)
    :class {:type :english                  ;; <- This is the English A class
            :name "English A"
            :teacher {:name "Mr. Anderson"  ;; <- The teacher is Mr. Anderson
                      :teaches  {:+/db/id (iid :Maths)}   ;;<- Oh and he also teaches Maths 
                      :canTeach :maths
                      :pets     :dog}}}     ;;<- He has a dog as a pet
   {:db/id (iid :EnglishB)
    :class {:type :english
            :name "English B"                   ;; <- The teacher for English B 
            :teacher {:name "Mr. Carpenter"     ;;    Mr Carpenter
                      :canTeach #{:sports :maths}
                      :teaches {:+/db/id (iid :Sports)}  ;; <- He also teaches Sport
                      :pets    #{:dog :fish :bird}}}}    ;; <- He has multiple pets
   {:db/id (iid :Sports)        
    :class {:type :sports          ;; <- Here is our Sports Class    
            :name "Sports"         ;; Since we already know that Mr. Carpenter teaches
            :accelerated false}}   ;; This subject, we don't need to put it in

   {:db/id (iid :Maths)
    :class {:type :maths
            :name "Maths"          ;; <- Oh, Mr. Anderson teaches this subject
            :accelerated true}}

   {:db/id (iid :Art)
    :class {:type :art
            :name "Art"            ;; <- Who is teaching Art I wonder?
            :accelerated true}}

   {:db/id (iid :Science)
    :class {:type :science
            :name "Science"
            :teacher {:name "Mr. Blair"
                      :teaches {:+/db/id (iid :Art)}   ;; <- Oh its Mr Blair, the science teacher 
                      :canTeach #{:maths :science}
                      :pets    #{:fish :bird}}}}

   {:db/id (iid :EnglishA)                   ;; And here are our students!
    :class/students #{{:name "Bobby"   
                       :siblings 2
                       :classes  {:+/db/id (iid :Maths)}}
                      {:name "David"
                       :siblings 5
                       :classes #{{:+/db/id (iid :Science)}
                                  {:+/db/id (iid :Maths)}}}
                      {:name "Erin"
                       :siblings 1
                       :classes #{{:+/db/id (iid :Art)}}}
                      {:name "Ivan"
                       :siblings 2
                       :classes #{{:+/db/id (iid :Science)}
                                  {:+/db/id (iid :Sports)}}}
                      {:name "Kelly"
                       :siblings 0
                       :classes #{{:+/db/id (iid :Science)}
                                  {:+/db/id (iid :Maths)}}}}}
   {:db/id (iid :EnglishB)
    :class/students #{{:name  "Anna"
                       :siblings 1
                       :classes #{{:+/db/id (iid :Sports)}
                                  {:+/db/id (iid :Art)}}}
                      {:name    "Charlie"
                       :siblings 3
                       :classes {:+/db/id (iid :Art)}}
                      {:name    "Francis"
                       :siblings 0
                       :classes #{{:+/db/id (iid :Art)}
                                  {:+/db/id (iid :Maths)}}}
                      {:name    "Harry"
                       :siblings 2
                       :classes #{{:+/db/id (iid :Art)}
                                  {:+/db/id (iid :Science)}
                                  {:+/db/id (iid :Maths)}}}
                      {:name    "Jack"
                       :siblings 4
                       :classes #{{:+/db/id (iid :Sports)}
                                  {:+/db/id (iid :Maths)}}}}}])
```

Okay... our data is defined.... and....


```clojure
(adi/insert! class-data class-datastore)
```

....***!!BAM!!*** .... We are now ready to query!!!

### Selecting

```clojure
;; A Gentle Intro
;;
;; Find the student with the name Harry

(adi/select {:student/name "Harry"} class-datastore) ;=> Returns a map with Harry

(-> ;; Lets get the database id of the student with the name Harry
 (adi/select {:student/name "Harry"} class-datastore)
 first :db :id) ;=>17592186045432 (Will be different)

(-> ;; Lets do the same with a standard datomic query
 (adi/select '[:find ?x :where
               [?x :student/name "Harry"]] class-datastore)
 first :db :id) ;=> 17592186045432 (The same)

;; More Advanced Queries
;;
;; Now lets query across objects:
;;
(->> ;; Find the student that takes sports
 (adi/select '[:find ?x :where
               [?x :student/classes ?c]
               [?c :class/type :sports]] class-datastore)
 (map #(-> % :student :name))) ;=> ("Ivan" "Anna" "Jack")

(->> ;; The same query with the object syntax 
 (adi/select {:student/classes/type :sports} class-datastore)
 (map #(-> % :student :name))) ;=> ("Ivan" "Anna" "Jack")

;; Full expressiveness on searches:
;;
(->> ;; Find the teacher that teaches a student called Harry
 (adi/select {:teacher/teaches/students/name "Harry"} class-datastore)
 (map #(-> % :teacher :name))) ;=> ("Mr. Anderson" "Mr. Carpenter" "Mr. Blair")

(->> ;; Find all students taught by Mr Anderson
 (adi/select {:student/classes/teacher/name "Mr. Anderson" } class-datastore)
 (map #(-> % :student :name))) ;=> ("Ivan" "Bobby" "Erin" "Kelly"
                               ;;   "David" "Harry" "Francis" "Jack")

(->> ;; Find all the students that have class with teachers with fish
 (adi/select {:student/classes/teacher/pets :fish } class-datastore)
 (map #(-> % :student :name)) sort)
;=> ("Anna" "Charlie" "David" "Francis" "Harry" "Ivan" "Jack" "Kelly")

(->> ;; Not that you'd ever want to write a query like this but you can!
     ;;
     ;;  Find the class with the teacher that teaches
     ;;  a student that takes the class taken by Mr. Anderson
 (adi/select {:class/teacher/teaches/students/classes/teacher/name
              "Mr. Anderson"} class-datastore)
 (map #(-> % :class :name))) ;=> ("English A" "Maths" "English B"
                             ;;   "Sports" "Art" "Science")

;; Contraints through addtional map parameters
;;
(->> ;; Find students that have less than 2 siblings and take art
 (adi/select {:student {:siblings (?q < 2) ;; <- WE CAN QUERY!!
                        :classes/type :art}} class-datastore)
 (map #(-> % :student :name))) ;=> ("Erin" "Anna" "Francis")

(->> ;; Find the classes that Mr Anderson teaches David
 (adi/select {:class {:teacher/name "Mr. Anderson"
                      :students/name "David"}} class-datastore)
 (map #(-> % :class :name))) ;=> ("English A" "Maths")
```

### Updating

```clojure
(-> ;; Find the number of siblings Harry has
 (adi/select {:student/name "Harry"} class-datastore)
 first :student :siblings) ;=> 2

(-> ;; His mum just had twins!
 (adi/update! {:student/name "Harry"} {:student/siblings 4} class-datastore))

(-> ;; Now how many sibling?
 (adi/select {:student/name "Harry"} class-datastore)
 first :student :siblings) ;=> 4
```

## Retractions

```clojure
(->> ;; Find all the students that have class with teachers with dogs
 (adi/select {:student/classes/teacher/pets :dog} class-datastore)
 (map #(-> % :student :name))
 sort)
;=> ("Anna" "Bobby" "Charlie" "David" "Erin" "Francis" "Harry" "Ivan" "Jack" "Kelly")

;;That teacher who teaches english-a's dog just died
(adi/retract! {:teacher/teaches/name "English A"}
              {:teacher/pets :dog} class-datastore)
(->> ;; Find all the students that have class with teachers with dogs
 (adi/select {:student/classes/teacher/pets :dog} class-datastore)
 (map #(-> % :student :name))
 sort)
;;=> ("Anna" "Charlie" "Francis" "Harry" "Ivan" "Jack")
```

### Deletions

```clojure
(->> ;; See who is playing sports
 (adi/select {:student/classes/type :sports} class-datastore)
 (map #(-> % :student :name)))
;=> ("Ivan" "Anna" "Jack")

;; Ivan went to another school
(adi/delete! {:student/name "Ivan"} class-datastore)

(->> ;; See who is left in the sports class
 (adi/select {:student/classes/type :sports} class-datastore)
 (map #(-> % :student :name)))
;=> ("Anna" "Jack")

;; The students in english A had a bus accident
(adi/delete! {:student/classes/name "English A"} class-datastore)

(->> ;; Who is left at the school
 (adi/select :student/name class-datastore)
 (map #(-> % :student :name)))
;=> ("Anna" "Charlie" "Francis" "Jack" "Harry")
```

## The Longer More Technical Version

Where I will try to go through some of the features of adi, and how its emitters work:

- The Scheme Map and Datomic Schema Emission
- Key Directory Paths as Map Accessors 
- The Datastore
- Data Representation and Datomic Data Emission
- Query Representation and Datomic Query Emission

## The Scheme Map

Scheme maps are a more expressive way to write schemas. Essentially, they are a shorthand form of how the data will be added and linked and are used to generate the datomic schema for the application as well as check the integrity of the data being added to datomic database.

We start off by defining a schema:


```clojure
(ns adi.intro
  (:require [adi.core :as adi]
            [adi.schema :as as]))

(def sm-account
  {:account {:username     [{:type        :string}]
             :password     [{:type        :string}]
             :permissions  [{:type        :keyword
                             :cardinality :many}]
             :points       [{:type        :long}]}})
```

Other options for the scheme-map are: (:unique :doc :index :fulltext :component? :no-history)
We can *emit* the datomic schema for the scheme map:

```clojure
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
```
The advantages to using the adi schema map is that it is much more readable and that we can link it to data

### Key directories as Map accessors 

you can define the schema multiple ways because the `/` operator is like a directory symbol

```clojure
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
```
 
### The Datastore

Next, we create a datastore, which in reality, is just a map containing a connection object and a schema.

```clojure
(def ds (adi/datastore sm-account "datomic:mem://adi-example" true true))

(keys ds)
;; => (:conn :options :schema)

(:conn ds) #<LocalConnection datomic.peer.LocalConnection@2b4a4d56>

(:options ds) ;=> {:defaults? true, :restrict? true, :required? true, :extras? false, :query? false, :sets-only? false}
```

### Data Insertion

Once a scheme map has been defined, now data can be added:

```clojure
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

(adi/insert! ds data-account)
;;=> .... datomic results ...
```

At a more primitive level, `insert!` relys on `emit-datoms` to generate data. There are different flags set for generating data for insertion as opposed to updating.

```clojure
(use '[adi.emit.datoms :only [emit-datoms-insert]]
(use '[adi.emit.process :only [process-init-env]])

(emit-datoms-insert data-account
  (process-init-env class-schema)

;;=> ({:db/id {:part :db.part/user, :idx -1000105},
;;     :account/password "a123",
;;     :account/username "alice",
;;     :account/points 0}
;;     :account/socialMedia {:part :db.part/user, :idx -1000110}]
;;; ...... ALOT OF RESULTS .....
;;    [:db/add {:part :db.part/user, :idx -1000115}
;;     :account/socialMedia {:part :db.part/user, :idx -1000114}])
```

## Querying Data

Now that there are some data in there, lets do some queries. It seems that having more choice in the way data is queried results in better programs. There are a couple of ways data can be queried:

By Datomic Queries:

```clojure
(adi/select
  '[:find ?e ?name
    :where
    [?e :account/username ?name]]
  ds)
```

By Id:

```clojure
(adi/select 17592186045421 ds)
```

By Hashmap:

```clojure
(adi/select {:account/permissions :editor} ds)

```

By Hashset (which returns the union of results):

```clojure
(adi/select #{17592186045421 {:account/permissions :editor}} ds)

```

This syntax is supported at by `emit-query`

```clojure
(use '[adi.emit.query :only [emit-query query-env]])

(emit-query {:account/permissions :editor} (query-env (process-init-env (sm-account))))
;;=> '[:find ?e1 :where
;;     [?e2 :account/permissions :editor]
;;     [?e2 :node/value "root"]]

(emit-query {:account/points #{(?q > 3) (?q < 6)}} (query-env s7-env))
;;=> '[:find ?e1 :where
;;     [?e1 :account/points ?e2]
;;     [(> ?e2 3)]
;;     [?e1 :account/points ?e3]
;;     [(< ?e3 6)]]
```

### Data Views

More on this when I have some examples. Basically, data views allow construction of any view of the data the programmer wants. See my tests especially [test_core.clj](https://github.com/zcaudate/adi/blob/master/test/adi/test_core.clj) for more details

### Future Work

- Automatic schema prediction
- Queries on datomic history
- More checks and properties on the schema

## License

Copyright © 2013 Chris Zheng

Distributed under the Eclipse Public License, the same as Clojure.
