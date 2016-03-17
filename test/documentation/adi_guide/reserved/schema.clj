(ns documentation.adi-guide.reserved.schema
  (:use midje.sweet)
  (:require [adi.core :as adi]))

[[:section {:title ":schema"}]]

[[:subsection {:title "bookstore"}]]

"`:schema` is the other part of the datastore that is created when `connect!` is called. The schema is a powerful ally because it contains information about how data should be related, created and managed. To demonstrate this, a bookstore schema is defined:"

(def schema-book
   {:book   {:name    [{:required true
                        :fulltext true}]
             :author  [{:fulltext true}]}})

"A connection is created using the schema:"

(def book-ds (adi/connect! "datomic:mem://adi-guide-schema-bookstore" schema-book true true))

"A single book is entered:"

(adi/insert! book-ds {:book {:name "The Magicians"
                             :author "Lev Grossman"}})

"And then accessed:"

(adi/select book-ds :book)
=> #{{:book {:name "The Magicians", :author "Lev Grossman"}}}

"We see that both the name and the author of the book is returned."

[[:subsection {:title "partial schema"}]]

"However, lets define a partial schema:"

(def name-schema (adi/schema {:book {:name  [{:required true}]}}))

"And make a call to select, passing in the `:schema` entry"

(adi/select book-ds :book :schema name-schema)
=> #{{:book {:name "The Magicians"}}}

"It can be seen that only the book name is returned."

[[:subsection {:title "more examples"}]]

"By passing in different schemas, an application can allow for isolation of various functionalities as well have as basic protection of data. It can be seen how the two schemas behave. Below shows a normal search on :book/author:"

(fact
  (adi/select book-ds :book/author)
  => #{{:book {:name "The Magicians", :author "Lev Grossman"}}})

"Using the abridged schema, an exception is thrown:"

(fact
  (adi/select book-ds :book/author :schema name-schema)
  => (throws))

"A search on stores return empty"
  
(fact
  (adi/select book-ds :store)
  => #{})

"Using the abridged schema, an exception is throw:"

(fact
  (adi/select book-ds :store :schema name-schema)
  => (throws))

