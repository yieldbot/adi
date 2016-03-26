(ns documentation.adi-guide
  (:use midje.sweet)
  (:require [adi.core :as adi]))

[[:chapter {:title "Introduction"}]]

"[adi](https://www.github.com/zcaudate/adi) provides a simple, intuitive data layer to access datomic using a document-based syntax, as well as a data-processing pipeline for fine-grain manipulation and access of data."

[[:section {:title "Installation"}]]

"
Add to `project.clj` dependencies:

    [im.chit/adi \"{{PROJECT.version}}\"]

All functionality is contained in the `adi.core` namespace:
"

(comment
  (require '[adi.core :as adi]))

[[:section {:title "Outline"}]]

"[adi](https://www.github.com/zcaudate/adi) provides the following advantages

- Using the schema as a 'type' system to process incoming data.
- Relations mapped to nested object structures (using a graph-like notion)
- Nested maps/objects as declarative logic queries.
- Custom views on data (similar to the `:pull` api)
"

[[:section {:title "Architecture"}]]

"The architecture can be seen below:"

[[:image {:src "img/adi.png" :width "100%"}]]

[[:chapter {:title "API"}]]

"Top Level API's are listed below, first, we define a schema:"

(def schema-api
 {:account {:user     [{:required true
                        :unique :value}]
            :books    [{:type :ref
                        :cardinality :many
                        :ref  {:ns :book}}]}
  :book   {:name    [{:required true
                      :fulltext true}]
           :author  [{:fulltext true}]}})

[[:section {:title "connect!"}]]

"`connect!` creates a connection to a datomic datastore:"

(def api-ds (adi/connect! "datomic:mem://adi-guide-api" schema-api true true))

[[:section {:title "insert!"}]]

"`insert!` puts data into the datastore:"

(adi/insert! api-ds
             [{:account {:user "Anne"
                         :books #{{:name "Watership Down"}}}}
              {:account {:user "Bob"
                         :books #{{:name "Canterbury Tales"}}}}])

[[:section {:title "select"}]]

"`select` retrieves from the datastore by category:"

(fact
  (adi/select api-ds :book)
  => #{{:book {:name "Watership Down"}}
       {:book {:name "Canterbury Tales"}}})

"or by the adi query syntax:"

(fact
  (adi/select api-ds {:book/name '_})
  => #{{:book {:name "Watership Down"}}
       {:book {:name "Canterbury Tales"}}})

[[:section {:title "query"}]]

"`query` retrieves results using a datomic style query"

(adi/query api-ds '[:find ?self :where
                    [?self :book/name _]]
           [])
=> #{{:book {:name "Watership Down"}}
     {:book {:name "Canterbury Tales"}}}

[[:section {:title "update!"}]]

"`update!` will take a query and update all datoms that match with additional values:"

(fact
  (adi/update! api-ds
               {:book/name "Watership Down"}
               {:book/author "Richard Adams"})

  (adi/select api-ds {:book/name "Watership Down"})
  => #{{:book {:name "Watership Down", :author "Richard Adams"}}})

[[:section {:title "retract!"}]]

"`retract!` will take a query and retract keys in all datoms that match"

(fact
  (adi/retract! api-ds
                {:book/name "Watership Down"}
                [:book/author])

  (adi/select api-ds {:book/name "Watership Down"})
  => #{{:book {:name "Watership Down"}}})

[[:section {:title "update-in!"}]]

"`update-in!` will take a query and update all datoms through the access path"

(fact
  (adi/update-in! api-ds
                  {:account/user "Anne"}
                  [:account/books {:name "Watership Down"}]
                  {:author "Richard Adams"})

  (adi/select api-ds {:book/name "Watership Down"})
  => #{{:book {:name "Watership Down", :author "Richard Adams"}}})

[[:section {:title "retract-in!"}]]

"`retract-in!` will take a query and retracts all keys through the access path"

(fact
  (adi/retract-in! api-ds
                  {:account/user "Anne"}
                  [:account/books {:name "Watership Down"}]
                  [:author])

  (adi/select api-ds {:book/name "Watership Down"})
  => #{{:book {:name "Watership Down"}}})


[[:section {:title "transact!"}]]

"`transact!` takes datomic datoms for update:"

(fact
  (adi/transact! api-ds
                 [{:db/id (adi/iid :charlie)
                   :account/user "Charlie"}])
  ;;=> [{:db {:id 17592186045423}
  ;;     :account {:user "Charlie"} }]

  (adi/select api-ds :account)
  => #{{:account {:user "Anne"}} {:account {:user "Bob"}} {:account {:user "Charlie"}}})

[[:section {:title "delete!"}]]

"`delete!` removes entities from the datastore:"

(fact
  (adi/delete! api-ds {:account/user "Charlie"})
  ;;=> #{{:db {:id 17592186045427}
  ;;      :account {:user "Charlie"}}}

  (adi/select api-ds :account)
  => #{{:account {:user "Anne"}} {:account {:user "Bob"}}})

[[:section {:title "delete-in!"}]]

"`delete-in!` will take a query and deletes all entities from the access path:"

(fact
  (adi/delete-in! api-ds {:account/user "Bob"}
                  [:account/books {:name '_}])
  ;;=> #{{:db {:id 17592186045421}
  ;;      :book {:name "Canterbury Tales"}}}

  
  (adi/select api-ds :book)
  => #{{:book {:name "Watership Down"}}})

[[:section {:title "delete-all!"}]]

"`delete-all!` will takes a query and deletes all entities govered by the access model:"

(fact
  (adi/delete-all! api-ds {:account/user "Anne"}
                   :access {:account {:books :checked}})
  ;;=> #{{:db {:id 17592186045418}}}
  ;;      :account  {:user "Anne",
  ;;                 :books #{{:name "Watership Down",
  ;;                           :+ {:db {:id 17592186045419}}}}}, 
  
   (adi/select api-ds :book)
  => #{})

[[:section {:title "Parameters"}]]

"Apart from the core query and transactional functions, there are many parameters and customisations that can be tweaked. This is very terse, but it is important for fine grain control of data. `adi` comes with a bunch of bells and whistles for data - some inherited from the underlying datomic api, others built to deal with the pipeline for schema-assisted transformation of data. There are keywords reserved by the top-level operations that can be set/overwritten through arguments.

Connection related entries:

- `:connection`
- `:db`
- `:at`
- `:return`
- `:transact`

Schema related entries:

- `:schema`
- `:pull`
- `:access`
"

[[:section {:title "Options"}]]

"There is a seperate param entry holding all the miscellaneous options, called (as you may have guessed) `options` and each sub-entry will be covered seperately:

- `:first`
- `:ids`
- `:ban-expressions`
- `:ban-ids`
- `:ban-top-id`
- `:ban-body-ids`
- `:raw`
- `:adi`
"

[[:section {:title "Pipeline"}]]

"The `:pipeline` entry has it's own set of sub-keys. They will be described in its own chapter:

- `:pre-process`
- `:pre-require`
- `:pre-mask`
- `:pre-transform`
- `:fill-empty`
- `:fill-assoc`
- `:ignore`
- `:allow`
- `:validate`
- `:convert`
- `:post-require`
- `:post-mask`
- `:post-transform`
- `:post-process`
"

[[:chapter {:title "Schema"}]]

[[:file {:src "test/documentation/adi_guide/schema.clj"}]]

[[:chapter {:title "Connection Params"}]]

[[:file {:src "test/documentation/adi_guide/reserved/connection.clj"}]]

[[:chapter {:title "Schema Params"}]]

[[:file {:src "test/documentation/adi_guide/reserved/schema.clj"}]]

[[:chapter {:title "Options"}]]

[[:file {:src "test/documentation/adi_guide/options.clj"}]]

[[:chapter {:title "Pipeline"}]]

"The data pipeline is used for preprocessing of incoming data before accessing datomic. Different stages of the pipeline can be seen below:"

[[:image {:src "img/adi-pipeline.png" :width "250px"}]]

[[:file {:src "test/documentation/adi_guide/reserved/pipeline.clj"}]]
