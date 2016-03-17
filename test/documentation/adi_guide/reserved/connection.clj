(ns documentation.adi-guide.reserved.connection
  (:use midje.sweet)
  (:require [adi.core :as adi]
            [datomic.api :as datomic]))

[[:section {:title ":connection"}]]

[[:subsection {:title "creating a datastore"}]]

"When `connect!` is called, it returns a datastore represented as a map containing entries for `:connection` and `:schema`:"

(def schema-connection
  {:book {:name    [{:required true
                     :fulltext true}]
          :author  [{:fulltext true}]}})


(def ds-1 (adi/connect! "datomic:mem://adi-guide-connection-1" schema-connection true true))

(println ds-1)
;;=> #adi{:connection #connection{1000 #inst "2016-03-15T12:47:10.039-00:00"},
;;        :schema     #schema{:book {:name :string, :author :string}}}

[[:subsection {:title "more datastores"}]]

"`:connection` is a reserved keyword in options. We can show how this works by creating two more datastores:"

(def ds-2 (adi/connect! "datomic:mem://adi-guide-connection-2" schema-connection true true))

(def ds-3 (adi/connect! "datomic:mem://adi-guide-connection-3" schema-connection true true))

"Passing a value for `:connection` will change the connection object that is used. This shouldn't be used very frequently but the feature is there to be exploited. To highlight this, a standard call to `insert!` is made:"

(fact
  (adi/insert! ds-1 {:book/name "Orpheus"})
  => (contains {:book {:name "Orpheus"}, :db map?}))

[[:subsection {:title "key/value input"}]]

"Lets see the different ways that the `:connection` entry can be overwritten, first via key/value pair:"

(fact
  (adi/insert! ds-1 {:book/name "Ulysses"} :connection (:connection ds-2))
  => (contains {:book {:name "Ulysses"}, :db map?}))

[[:subsection {:title "map input"}]]

"The arguments can also be passed in as part of a map:"

(fact
  (adi/insert! ds-1  {:book/name "Eurydice"} {:connection (:connection ds-3)})
  => (contains {:book {:name "Eurydice"}, :db map?}))

[[:subsection {:title "different sinks"}]]

"It can be seen that each datastore has a book of their own:"

(facts
 (adi/select ds-1 :book)
  => #{{:book {:name "Orpheus"}}}


  (adi/select ds-2 :book)
  => #{{:book {:name "Ulysses"}}}

  (adi/select ds-3 :book)
  => #{{:book {:name "Eurydice"}}})

[[:subsection {:title "update example"}]]

"Examples below show overwriting of `:connection` for top level operations such as `update!`"

(fact 
  (adi/update! ds-1 :book {:book/name "Medea"} (select-keys ds-3 [:connection]))
  (adi/select ds-3 :book)
  => #{{:book {:name "Medea"}}})

[[:subsection {:title "delete example"}]]

"An example for `delete!` has alse beeen shown:"

(fact
  (adi/delete! (assoc ds-1 :connection (:connection ds-3)) :book)
  (adi/select ds-3 :book)
  => #{})

[[:section {:title ":db"}]]

"For searches (`select`), instead of specifying a `:connection` entry, a `:db` entry can be passed. The following are equivalent calls:"

(fact
  (adi/select ds-1 :book)
  => #{{:book {:name "Orpheus"}}})

[[:subsection {:title "key/value input"}]]

"`:db` passed in as args"

(fact
  (adi/select ds-1 :book :db (datomic/db (:connection ds-1)))
  => #{{:book {:name "Orpheus"}}})

[[:subsection {:title "map input"}]]

"`:db` passed in as a map"

(fact
  (adi/select ds-1 :book {:db (datomic/db (:connection ds-1))})
  => #{{:book {:name "Orpheus"}}})

[[:subsection {:title "overwriting"}]]

"Like `:connection`, the entry for `:db` can be something completely unrelated to the original `:db` object, in this case, we are searching on `ds-2`:"

(fact
  (adi/select ds-1 :book {:db (datomic/db (:connection ds-2))})
  => #{{:book {:name "Ulysses"}}})


[[:section {:title ":at"}]]

[[:subsection {:title "time travel"}]]

"[datomic](http://www.datomic.com/) allows searching of the database at any point in time. This functionality is accessible through the `:at` keyword. We can see this in action:"

(fact
  (adi/select ds-1 :book :at 0)
  => #{})

[[:subsection {:title "syntactic sugar"}]]

"`:at` makes the intent more clear. The previous statement is equivalent to:"

(fact
  (adi/select ds-1 :book {:db (-> (:connection ds-1)
                                  (datomic/db) 
                                  (datomic/as-of 0))})
  => #{})

[[:subsection {:title "transaction id"}]]

"We can query the datastore after the first transaction has occured, by using an id of `1001` to access the datastore."

(fact
  (adi/select ds-1 :book :at 1001)
  => #{{:book {:name "Orpheus"}}})

"Again, this is equivalent to:"

(fact
  (adi/select ds-1 :book {:db (-> (:connection ds-1)
                                  (datomic/db) 
                                  (datomic/as-of 1001))})
  => #{{:book {:name "Orpheus"}}})

[[:subsection {:title "transaction time"}]]

"We can query the datastore after the first transaction has occured through the time parameter:"

(comment
  (:connection ds-1)
  ;;=> #connection{1001 #inst "2016-03-15T19:13:37.085-00:00"}

  (adi/select ds-1 :book :at #inst "2016-03-15T19:13:00.000-00:00")
  ;;=> #{}

  (adi/select ds-1 :book :at #inst "2016-03-15T19:14:00.000-00:00")
  ;;=> #{{:book {:name "Orpheus"}}}
  )

[[:subsection {:title "mix and match"}]]

"The `:at` entry can be used in conjunction with `:db`:"

(fact
  (adi/select ds-1 :book {:db (datomic/db (:connection ds-2))
                          :at 0})
  => #{})

"as well as `:connection`:"

(fact
  (adi/select ds-1 :book {:connection (:connection ds-2)
                          :at 1001})
  => #{{:book {:name "Ulysses"}}})


[[:section {:title ":return"}]]

"`:return` determines the data that has been queried. This is only for `select` and there are three options available:

- `:ids`
- `:entities`
- `:data`"

[[:subsection {:title ":ids"}]]

"Returns the set of datomic ids that matches the query;"

(fact
  (adi/select ds-1 :book :return :ids)
  => #{17592186045418})

[[:subsection {:title ":entities"}]]

"Returns the set of datomic entities that matches the query;"

(fact
  (adi/select ds-1 :book :return :entities)
  => #{{:db/id 17592186045418}})

[[:subsection {:title ":data"}]]

"The default option, returns actual data that can be governed by entries in `:pull` and `:model`"

(fact
  (adi/select ds-1 :book :return :data)
  => #{{:book {:name "Orpheus"}}})


[[:section {:title ":transact"}]]

"`:transact` determines how the results of the call is returned to the caller. This is an important to tune for mutation operations to place emphasis on correctness or speed. There are four options available:

- `:resolve`
- `:datomic`
- `:promise`
- `:async`"

[[:subsection {:title ":resolve"}]]

"This is the default option and takes the longest time. Ensures that that generated temporary ids are resolved and that the data can be used in further transactions:"

(comment
  (adi/insert! ds-1 {:book/name "The Magic School Bus"} {:transact :resolve})
  ;;=> {:book {:name "The Magic School Bus"},
  ;;    :db {:id 17592186045432}}
  )

[[:subsection {:title ":datomic"}]]

"Waits for the result of the transaction to come back before returning the results:"

(comment
  (adi/insert! ds-1 {:book/name "The Magic School Bus"} {:transact :datomic})
  ;;=> {:db-before #db{1007 #inst "2016-03-16T12:23:35.761-00:00"},
  ;;    :db-after #db{1009 #inst "2016-03-16T12:25:42.566-00:00"},
  ;;    :tx-data [#datom[13194139534321 50 #inst "2016-03-16T12:25:42.566-00:00" 13194139534321 true]
  ;;              #datom[17592186045426 63 "The Magic School Bus" 13194139534321 true]],
  ;;    :tempids {-9223350046901838717 17592186045426}}
  )

[[:subsection {:title ":promise"}]]

"Wraps datomic's `transact` call to return a promise:"

(comment
  (adi/insert! ds-1 {:book/name "The Magic School Bus"} {:transact :promise})
  ;;=> #promise
  ;;    {:status :ready,
  ;;     :val {:db-before #db{1005 #inst "2016-03-16T12:23:15.992-00:00"},
  ;;           :db-after #db{1007 #inst "2016-03-16T12:23:35.761-00:00"},
  ;;           :tx-data [#datom[13194139534319 50 #inst "2016-03-16T12:23:35.761-00:00" 13194139534319 true]
  ;;                     #datom[17592186045424 63 "The Magic School Bus" 13194139534319 true]],
  ;;           :tempids {-9223350048542385808 17592186045424}}}
)

[[:subsection {:title ":async"}]]

"Wraps datomic's `transact-async` call to return a promise:"

(comment
  (adi/insert! ds-1 {:book/name "The Magic School Bus"} {:transact :async})
  ;;=> #promise
  ;;    {:status :ready,
  ;;     :val {:db-before #db{1005 #inst "2016-03-16T08:53:44.894-00:00"},
  ;;           :db-after #db{1007 #inst "2016-03-16T08:53:48.645-00:00"},
  ;;           :tx-data [#datom[13194139534319 50 #inst "2016-03-16T08:53:48.645-00:00" 13194139534319 true]
  ;;                     #datom[17592186045424 63 "The Magic School Bus" 13194139534319 true]],
  ;;           :tempids {-9223350047214995662 17592186045424}}}
)
