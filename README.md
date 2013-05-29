# adi

`adi`, rhyming with 'hardy' stands for the acronym (a) (d)atomic (i)nterface. It has four main objectives

- Using the schema as a 'type' system to process incoming data.
- Relations mapped to nested object structures (using a graph-like notion)
- Nested maps/objects as declarative logic queries.
- Custom views on data (schemas for how data is consumed)

The concept is simple. `adi` is a document-database syntax grafted on Datomic. It makes use of a map/object notation to generate datastructure for Datomic's query engine. This provides for an even more declarative syntax for relational search. Fundamentally, there should be no difference in the data-structure between what the programmer uses to ask  and what the programmer is getting back. We shouldn't have to play around turning objects into records, objects into queries... etc...

***Not Anymore.***

Datomic began something brand new for data, and `adi` leverages that incredible flexiblility with a syntax that is simple to understand.  It converts flat, record-like arrays to tree-like objects and back again so that the user can interface with Datomic the way that expresses Datomic's true capabilities.

The key to understanding `adi` lies in understanding the power of a schema. The schema dictates what you can do with the data. Instead of limiting the programmer, the schema should exhance him/her, much like what a type-system does for programmers - without being suffocatingly restrictive. Once a schema for an application has been defined, the data can be inserted in ANY shape, as long as it follows the coventions specified within that schema.


#### Installation

In your project file, add

```clojure
[adi "0.1.5"]
```

### TODOS
- Break up the readme into sizable chunks
- Do self reference tutorials

### First Steps

Lets show off some things we can do with adi. We start off by defining a user account and gradually add more features as we go along. 

#### Step 1
Fire up nrepl/emacs/vim and load `adi.core`.
```clojure
(require '[adi.core :as adi])
```
We first have to define a `geni`, which is a template for our data:
```clojure            
(def geni-1
 {:account/user     [{:type :string      ;; (1)
                      :cardinality :one  
                      :unique :value     ;; (2)
                      :required true}]   ;; (3)
  :account/password [{:required true     ;; (1) (3)
                      :restrict ["password needs an integer to be in the string" 
                                   #(re-find #"\d" %)] ;; (4)
  :account/credits  [{:type :long     
                      :default 0}]}]}})   ;; (5)
```
There are a couple of things to note about our definitions the entry.
  1. We specified the `:type` for `:account/user` to be `:string` and `:cardinality` to be `:one`. However, because these are default options, we can optionally leave them out for `:account/password`.
  2. We want the value of `:account/user` to be unique.
  3. We require that both `:account/user` and `:account/password` to be present on insertion.
  4. We are checking that `:account/password` contains at least one number
  5. We set the default amount of credits in the account to be 0
  
Now, we construct a datastore. 
```clojure            
(def ds (adi/datastore "datomic:mem://example-1" geni-1 true true))
```
The parameters are:
   - *uri* - The uri of the datomic database to connect to
   - *geni* - The previously defined data template 
   - *install?* - Optional flag (if true, will install the `geni` into the database) 
   - *recreate?* - Optional flag (if true will delete and then create the database)

##### Trial and Error
So lets attempt to add some data. We'll do this via trial and error:
```clojure
(adi/insert! ds {:account {:credits 10}})
;; => (throws Exception "The following keys are required: #{:account/user :account/password}")

(adi/insert! ds {:account {:user "adi"}})
;;=> (throws Exception "The following keys are required: #{:account/password}")

(adi/insert! ds {:account {:user "adi" :password "hello"}})
;;=> (throws Exception "The value hello does not meet the restriction: password needs an integer to be in the string")

(adi/insert! ds {:account {:user "adi" :password "hello1" :type :vip}})
;;=> (throws Exception "(:type :vip) not in schema definition")

(adi/insert! ds {:account {:user "adi" :password "hello1"}})
;;=> Finally, No Errors! Our data is finally installed. Lets do one more:

(adi/insert! ds {:account {:user "adi" :password "hello2" :credits 10}})
;;=> (throws Exception "ExceptionInfo :transact/bad-data Unique conflict: :account/user, value: adi already held")

(adi/insert! ds {:account {:user "adi2" :password "hello2" :credits 10}})
;;=> Okay, another record inserted!
```

##### Selections
We can now have a play with the data:
```clojure
(adi/select ds :account)
;;=> ({:db {:id 17592186045418}, :account {:user "adi", :password "hello1", :credits 0}}
;;    {:db {:id 17592186045420}, :account {:user "adi2", :password "hello2", :credits 10}})

(adi/select ds :account :hide-ids) ;; We can hide ids for ease of view
;;=> ({:account {:user "adi", :password "hello1", :credits 0}}
;;    {:account {:user "adi2", :password "hello2", :credits 10}})

(adi/select ds {:account/user "adi"} :first :hide-ids)
;;=> {:account {:user "adi", :password "hello1", :credits 0}}

;; We can also look at transactions when for a particular attribute has been changed
(adi/transactions ds :account/user)
;;=> (1001 1003)

;; We can also look at specific values of the changed attributes
(adi/transactions ds :account/user "adi")
;;=> (1001)

;; We can also select our data at the point of the actual transaction
(adi/select ds :account :at 1001 :hide-ids)
;;=> ({:account {:user "adi", :password "hello1", :credits 0}})
```

#### Step 2
So lets add one more field - the `:enum` type

```clojure
(def geni-2           ;; (1)
 {:account {:user     [{:required true
                        :unique :value}]
            :password [{:required true
                        :restrict ["password needs an integer to be in the string"
                                   #(re-find #"\d" %)]}]
            :credits  [{:type :long
                        :default 0}]
            :type     [{:type :enum        ;; (2)
                        :default :free
                        :enum {:ns :account.type
                               :values #{:admin :free :paid}}}]}})
```
Comments
 1. Note that instead of using `:account/<attr>` way in Step 1 to specify attributes, we can just nest the attributes under the account `:account` namespace. This allows for much better readability.
   
 2. The enum type is a special :ref type that is defined here http://docs.datomic.com/schema.html#sec-3. adi makes it easy to install and manage them. We put them under a common namespace (:account.type) and give them values #{:admin :free :paid}


##### enums
Lets explore enums a little bit more by going under the covers of adi. Using datomic,
we can see that the enums have been installed as datomic refs:

```clojure
(require '[datomic.api :as d])
(def ds (adi/datastore "datomic:mem://example-2" geni-2 true true))

(d/q '[:find ?x :where
       [?x :db/ident :account.type/free]]
     (d/db (:conn ds)))
;;=> #{[17592186045417]}

(d/q '[:find ?x :where
       [?x :db/ident :account.type/paid]]
     (d/db (:conn ds)))
;;=> #{[17592186045418]}
```
Lets insert some data. We can insert multiple records in one transaction:
```clojure
(adi/insert! ds [{:account {:user "adi1"          ;; (1)
                            :password "hello1"}
                  :account/type :paid}            ;; (2)
                 {:account {:password "hello2" :type
                            :account.type/admin}  ;; (2)
                  :account/user "adi2"}
                 {:account {:user "adi3"
                            :credits 1000}
                  :account/password "hello3"}])
```
Comments
  1. data can be formatted arbitrarily as long as the `/` is consistent with the level of map nesting. This is a design decision to allow maximal readability.
  2. enums can be specified fully `:account.type/<value>` or as just `:<value>`, also they will always be outputted as the full version.

##### more selections
We can play with the data again:
```clojure
(adi/select ds :account :hide-ids)
;;=> ({:account {:user "adi1", :password "hello1", :credits 0, :type :account.type/paid}}
;;    {:account {:user "adi2", :password "hello2", :credits 0, :type :account.type/admin}}
;;    {:account {:user "adi3", :password "hello3", :credits 1000, :type :account.type/free}})

(adi/select ds {:account/type :admin} :first :hide-ids)
;;=> {:account {:user "adi2", :password "hello2", :credits 0, :type :account.type/admin}}

(adi/select ds {:account/credits 1000} :first :hide-ids)
;;=> {:account {:user "adi3", :password "hello3", :credits 1000, :type :account.type/free}}

;; We can use function within our queries to filter our data

(adi/select ds {:account/credits '(> 10)} :first :hide-ids)
;;=> {:account {:user "adi3", :password "hello3", :credits 1000, :type :account.type/free}}

(adi/select ds {:account/credits '(> ? 10)} :first :hide-ids)
;;=> {:account {:user "adi3", :password "hello3", :credits 1000, :type :account.type/free}}

(adi/select ds {:account/credits '(< 10 ?)} :first :hide-ids)
;;=> {:account {:user "adi3", :password "hello3", :credits 1000, :type :account.type/free}}

;; Note that the above three give the same results. If there is no `?`, it is assumed that 
;; the first argument is `?`. This is also seen below:

(adi/select ds {:account/user '(.contains "2")} :first :hide-ids)
;;=> {:account {:user "adi2", :password "hello2", :credits 0, :type :account.type/admin}}

(adi/select ds {:account/user '(.contains ? "2")} :first :hide-ids)
;;=> {:account {:user "adi2", :password "hello2", :credits 0, :type :account.type/admin}}

(adi/select ds {:account/user '(.contains "adi222" ?)} :first :hide-ids)
;;=> {:account {:user "adi2", :password "hello2", :credits 0, :type :account.type/admin}}
```

##### additional commands

Lets look at transactions again:
```clojure
(adi/transactions ds :account/user)
;;=> (1004) 

(adi/select ds :account :at 1003)
;;=> '()

;; enums are automatically restricted
 
(adi/insert! ds {:account {:user "adi4"
                           :password "hello4"
                           :type :vip}})
;;=>  (throws Exception "The value :vip does not meet the restriction: #{:free :paid :admin}")
```

#### Step 3

Lets go one step further and start using `refs` in our application. We have 
```clojure
(def geni-3
 {:account {:user     [{:required true
                        :unique :value}]
            :password [{:required true
                        :restrict ["password needs an integer to be in the string"
                                   #(re-find #"\d" %)]}]
            :type     [{:type :enum
                        :default :free
                        :enum {:ns :account.type
                               :values #{:admin :free :paid}}}]
            :credits  [{:type :long
                        :default 0}]
            :books    [{:type :ref
                        :cardinality :many
                        :ref  {:ns :book}}]}
  :book   {:name    [{:required true
                      :fulltext true}]
           :author  [{:fulltext true}]}})

(def ds (adi/datastore "datomic:mem://example-3" geni-3 true true))
```
Again, we insert some data:

```clojure
(adi/insert! ds
         [{:account {:user "adi1" :password "hello1"}}
          {:account {:user "adi2" :password "hello2"
                     :books #{{:name "The Count of Monte Cristo"
                               :author "Alexander Dumas"}
                              {:name "Tom Sawyer"
                               :author "Mark Twain"}
                              {:name "Les Misérables"
                               :author "Victor Hugo"}}}}])

(adi/select ds :account :view #{:account/books} :hide-ids)
;;=> ({:account {:user "adi1", :password "hello1", :credits 0, :type :account.type/free}}
;;    {:account {:user "adi2", :password "hello2", :credits 0,
;;           :books #{{:author "Alexander Dumas", :name "The Count of Monte Cristo"}
;;                   {:author "Mark Twain", :name "Tom Sawyer"}
;;                   {:author "Victor Hugo", :name "Les Misérables"}},
;;           :type :account.type/free}})
```
We can insert book information through accounts. We can also insert account information through books.

```clojure
(def users (adi/select-ids ds :account))

(adi/insert! ds [{:book {:name "Charlie and the Chocolate Factory"
                         :author "Roald Dahl"
                         :accounts #{{:user "adi3" :password "hello3" :credits 100}
                                     {:user "adi4" :password "hello4" :credits 500}
                                     {:user "adi5" :password "hello5" :credits 500}}}}
                 {:book {:name "The Book and the Sword"
                         :author "Louis Cha"
                         :accounts users}}])
```
Again, lets play with the data

```clojure
(adi/select ds {:account/user "adi1"} :view #{:account/books} :first :hide-ids)
;;=> {:account {:user "adi1", :password "hello1", :credits 0,
;;           :books #{{:author "Louis Cha", :name "The Book and the Sword"}}, :type :account.type/free}}

(adi/select ds {:account/user "adi1"} :first :hide-ids :view {:account/books :show})
;;=> {:account {:user "adi1", :password "hello1", :credits 0,
;;           :books #{{:+ {:db {:id 17592186045431}}}}, :type :account.type/free}}

(adi/select ds {:account/user "adi1"} :first :hide-ids :view {:account/books :follow})
;;=> {:account {:user "adi1", :password "hello1", :credits 0,
;;              :books #{{:author "Louis Cha", :name "The Book and the Sword"}}, :type :account.type/free}}

(adi/select ds {:account/user "adi1"} :first :hide-ids :view {:account/books :follow
                                                              :account/user :hide
                                                              :account/password :hide
                                                              :account/credits :hide
                                                              :account/type :hide})
;;=> {:account {:books #{{:author "Louis Cha", :name "The Book and the Sword"}}}}

(adi/select ds {:account/user "adi3"} :view #{:account/books} :first :hide-ids)
;;=> {:account {:user "adi3", :password "hello3", :credits 100,
;;           :books #{{:author "Roald Dahl", :name "Charlie and the Chocolate Factory"}}, :type :account.type/free}}

(adi/select ds {:book/author '(.startsWith ? "Mark")} :hide-ids :first)
;;=> {:book {:author "Mark Twain", :name "Tom Sawyer"}}

(adi/select ds {:book/author '(?fulltext "Louis")} :view #{:book/accounts} :hide-ids :first)
;;=>  {:book {:author "Louis Cha", :name "The Book and the Sword",
;;        :accounts #{{:user "adi2", :password "hello2", :credits 0, :type :account.type/free}
;;                   {:user "adi1", :password "hello1", :credits 0, :type :account.type/free}}}}
```

Here, we find all the books that user "adi2" has

```clojure
(adi/select ds {:book/accounts/user "adi2"} :hide-ids)
;;=> ({:book {:author "Alexander Dumas", :name "The Count of Monte Cristo"}}
;;  {:book {:author "Mark Twain", :name "Tom Sawyer"}}
;;  {:book {:author "Victor Hugo", :name "Les Misérables"}}
;;  {:book {:author "Louis Cha", :name "The Book and the Sword"}})
```
We find all users that have a book with a name that contains `the`.
```clojure
(adi/select ds {:account/books/name '(.contains ? "the")} :hide-ids)
;;=> ({:account {:user "adi1", :password "hello1", :credits 0, :type :account.type/free}}
;;    {:account {:user "adi2", :password "hello2", :credits 0, :type :account.type/free}}
;;    {:account {:user "adi3", :password "hello3", :credits 100, :type :account.type/free}}
;;    {:account {:user "adi4", :password "hello4", :credits 500, :type :account.type/free}}
;;    {:account {:user "adi5", :password "hello5", :credits 500, :type :account.type/free}})
```

### Modelling Example

This is a longish tutorial, mainly because of the data we have to write:

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
                                 :rval :students}   ;; Same with students 
                         :cardinality :many}]}})
```

Here, we create a `datastore`, which is a thin wrapper around the datomic connection object

```clojure
(def class-datastore
  (adi/datastore "datomic:mem://class-datastore" class-schema true true))
```

Now is the fun part: Lets fill in the data. This is one way of filling out the data. There are many other ways. Note that it is object-like in nature, with links defined through ids. If it doesn't contain an id, the record is automatically created. The example is slightly contrived mainly to show-off some different features of `adi`:

```clojure
(def class-data                      ;;; Lets See....
  [{:db/id (iid :Maths)
    :class {:type :maths             ;;; There's Math. The most important subject
            :name "Maths"            ;;; We will be giving all the classes ids 
            :accelerated true}}      ;;; for easier reference
    
    {:db/id (iid :Science)           ;;; Lets add science 
     :class {:type :science
             :name "Science"}}
    
    {:student {:name "Ivan"          ;;; And then Ivan, who does English, Science and Sports 
           :siblings 2
           :classes #{{:+/db/id (iid :EnglishA)}
                      {:+/db/id (iid :Science)}
                      {:+/db/id (iid :Sports)}}}}

    {:teacher {:name "Mr. Blair"                       ;; Here's Mr Blair
               :teaches #{{:+/db/id (iid :Art)      
                           :type :art                  ;; He teaches Art  
                           :name "Art"
                           :accelerated true}
                          {:+/db/id (iid :Science)}}   ;; He also teaches Science
               :canTeach #{:maths :science}
               :pets    #{:fish :bird}}}               ;; And a fish and a bird

    {:teacher {:name "Mr. Carpenter"                   ;; This is Mr Carpenter
               :canTeach #{:sports :maths}
               :pets    #{:dog :fish :bird}
               :teaches #{{:+/db/id (iid :Sports)      ;; He teaches sports
                           :type :sports
                           :name "Sports"
                           :accelerated false
                           :students #{{:name "Jack"   ;; There's Jack
                                        :siblings 4    ;; Who is also in EnglishB and Maths
                                        :classes #{{:+/db/id (iid :EnglishB)
                                                    :students {:name  "Anna"  ;; There's also Anna in the class
                                                               :siblings 1    
                                                               :classes #{{:+/db/id (iid :Art)}}}}
                                                                          {:+/db/id (iid :Maths)}}}}}
                          {:+/db/id (iid :EnglishB)    
                           :type :english             ;; Now we revisit English B
                           :name "English B"          ;;  Here are all the additional students
                           :students #{{:name    "Charlie"
                                        :siblings 3
                                        :classes  #{{:+/db/id (iid :Art)}}}
                                       {:name    "Francis"
                                        :siblings 0
                                        :classes #{{:+/db/id (iid :Art)}
                                                   {:+/db/id (iid :Maths)}}}
                                       {:name    "Harry"
                                        :siblings 2
                                        :classes #{{:+/db/id (iid :Art)}
                                                   {:+/db/id (iid :Science)}
                                                   {:+/db/id (iid :Maths)}}}}}}}}
    Phew.... So what are we missing?
                               
    {:db/id (iid :EnglishA)       ;; What about Engilsh A ?
     :class {:type :english
             :name "English A"
             :teacher {:name "Mr. Anderson" ;; Mr Anderson is the teacher
                       :teaches  {:+/db/id (iid :Maths)} ;; He also takes Maths
                       :canTeach :maths
                       :pets     :dog}
             :students #{{:name "Bobby"   ;; And the students are listed
                          :siblings 2
                          :classes  {:+/db/id (iid :Maths)}}
                         {:name "David"
                          :siblings 5
                          :classes #{{:+/db/id (iid :Science)}
                                     {:+/db/id (iid :Maths)}}}
                         {:name "Erin"
                          :siblings 1
                          :classes #{{:+/db/id (iid :Art)}}}
                         {:name "Kelly"
                          :siblings 0
                          :classes #{{:+/db/id (iid :Science)}
                                     {:+/db/id (iid :Maths)}}}}}}])
```

Okay... our data is defined... and...

```clojure
(adi/insert! class-datastore class-data)
```

***BAM!!***... We are now ready to query!!!

### Selecting

```clojure
;; A Gentle Intro
;;
;; Find the student with the name Harry

(adi/select class-datastore {:student/name "Harry"}) ;=> Returns a map with Harry

(-> ;; Lets get the database id of the student with the name Harry
 (adi/select class-datastore {:student/name "Harry"})
 first :db :id) ;=>17592186045432 (Will be different)

(-> ;; Lets do the same with a standard datomic query
 (adi/select class-datastore
             '[:find ?x :where
               [?x :student/name "Harry"]])
 first :db :id) ;=> 17592186045432 (The same)

;; More Advanced Queries
;;
;; Now lets query across objects:
;;
(->> ;; Find the student that takes sports
 (adi/select  class-datastore
             '[:find ?x :where
               [?x :student/classes ?c]
               [?c :class/type :sports]])
 (map #(-> % :student :name))) ;=> ("Ivan" "Anna" "Jack")

(->> ;; The same query with the keyword syntax 
 (adi/select class-datastore {:student/classes/type :sports})
 (map #(-> % :student :name))) ;=> ("Ivan" "Anna" "Jack")

(->> ;; The same query with the object syntax
  (adi/select class-datastore {:student {:classes {:type :sports}}})
  (map #(-> % :student :name))) ;=> ("Ivan" "Anna" "Jack")

;; The following are equivalent:
(= (adi/select class-datastore {:student/classes {:type :sports}})
   (adi/select class-datastore {:student {:classes/type :sports}})
   (adi/select class-datastore {:student/classes/type :sports})
   (adi/select class-datastore {:student {:classes {:type :sports}}}))


;; Full expressiveness on searches:
;;
(->> ;; Find the teacher that teaches a student called Harry
 (adi/select class-datastore {:teacher/teaches/students/name "Harry"})
 (map #(-> % :teacher :name))) ;=> ("Mr. Anderson" "Mr. Carpenter" "Mr. Blair")

(->> ;; Find all students taught by Mr Anderson
 (adi/select class-datastore {:student/classes/teacher/name "Mr. Anderson" })
 (map #(-> % :student :name))) ;=> ("Ivan" "Bobby" "Erin" "Kelly"
                               ;;   "David" "Harry" "Francis" "Jack")

(->> ;; Find all the students that have class with teachers with fish
 (adi/select class-datastore {:student/classes/teacher/pets :fish })
 (map #(-> % :student :name)) sort)
;=> ("Anna" "Charlie" "David" "Francis" "Harry" "Ivan" "Jack" "Kelly")

(->> ;; Not that you'd ever want to write a query like this but you can!
     ;;
     ;;  Find the class with the teacher that teaches
     ;;  a student that takes the class taken by Mr. Anderson
 (adi/select  class-datastore   {:class/teacher/teaches/students/classes/teacher/name
              "Mr. Anderson"})
 (map #(-> % :class :name))) ;=> ("English A" "Maths" "English B"
                             ;;   "Sports" "Art" "Science")

;; Contraints through addtional map parameters
;;
(->> ;; Find students that have less than 2 siblings and take art
 (adi/select class-datastore
    {:student {:siblings (?q < 2) ;; <- WE CAN QUERY!!
               :classes/type :art}})
 (map #(-> % :student :name))) ;=> ("Erin" "Anna" "Francis")

(->> ;; Find the classes that Mr Anderson teaches David
 (adi/select class-datastore
             {:class {:teacher/name "Mr. Anderson"
                      :students/name "David"}})
 (map #(-> % :class :name))) ;=> ("English A" "Maths")
```

### Updating

```clojure
(-> ;; Find the number of siblings Harry has
 (adi/select class-datastore {:student/name "Harry"})
 first :student :siblings) ;=> 2

(-> ;; His mum just had twins!
 (adi/update! class-datastore {:student/name "Harry"} {:student/siblings 4}))

(-> ;; Now how many sibling?
 (adi/select class-datastore {:student/name "Harry"})
 first :student :siblings) ;=> 4
```

## Retractions

```clojure
(->> ;; Find all the students that have class with teachers with dogs
 (adi/select class-datastore {:student/classes/teacher/pets :dog})
 (map #(-> % :student :name))
 sort)
;=> ("Anna" "Bobby" "Charlie" "David" "Erin" "Francis" "Harry" "Ivan" "Jack" "Kelly")

;;That teacher who teaches english-a's dog just died
(adi/retract! class-datastore
              {:teacher/teaches/name "English A"}
              {:teacher/pets :dog})
(->> ;; Find all the students that have class with teachers with dogs
 (adi/select class-datastore {:student/classes/teacher/pets :dog})
 (map #(-> % :student :name))
 sort)
;;=> ("Anna" "Charlie" "Francis" "Harry" "Ivan" "Jack")
```

### Deletions

```clojure
(->> ;; See who is playing sports
 (adi/select class-datastore {:student/classes/type :sports})
 (map #(-> % :student :name)))
;=> ("Ivan" "Anna" "Jack")

;; Ivan went to another school
(adi/delete! class-datastore {:student/name "Ivan"})

(->> ;; See who is left in the sports class
 (adi/select class-datastore {:student/classes/type :sports})
 (map #(-> % :student :name)))
;=> ("Anna" "Jack")

;; The students in english A had a bus accident
(adi/delete! class-datastore {:student/classes/name "English A"})

(->> ;; Who is left at the school
 (adi/select class-datastore :student/name)
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
(adi/select ds 17592186045421)
```

By Hashmap:

```clojure
(adi/select ds {:account/permissions :editor})

```

By Hashset (which returns the union of results):

```clojure
(adi/select ds #{17592186045421 {:account/permissions :editor}})

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
