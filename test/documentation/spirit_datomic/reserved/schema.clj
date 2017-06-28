(ns documentation.spirit-datomic.reserved.schema
  (:use hara.test)
  (:require [spirit.datomic :as datomic]))

[[:section {:title ":schema"}]]

[[:subsection {:title "book"}]]

"`:schema` is the other part of the datastore that is created when `connect!` is called. The schema is a powerful ally because it contains information about how data should be related, created and managed. To demonstrate this, a book schema is defined:"

(def schema-book
   {:book   {:name    [{:required true
                        :fulltext true}]
             :author  [{:fulltext true}]}})

"A connection is created using the schema:"

(def book-ds (datomic/connect!
              {:uri "datomic:mem://datomic-schema-book"
               :schema schema-book
               :options {:install-schema true
                         :reset-db true}}))

"A single book is entered:"

(datomic/insert! book-ds {:book {:name "The Magicians"
                                 :author "Lev Grossman"}})

"And then accessed:"

(fact
  (datomic/select book-ds :book)
  => #{{:book {:name "The Magicians", :author "Lev Grossman"}}})

"We see that both the name and the author of the book is returned."

[[:subsection {:title "partial schema"}]]

"However, lets define a partial schema:"

(def name-schema (datomic/schema {:book {:name  [{:required true}]}}))

"And make a call to select, passing in the `:schema` entry"

(fact
  (datomic/select book-ds :book :schema name-schema)
  => #{{:book {:name "The Magicians"}}})

"It can be seen that only the book name is returned."

[[:subsection {:title "more examples"}]]

"By passing in different schemas, an application can allow for isolation of various functionalities as well have as basic protection of data. It can be seen how the two schemas behave. Below shows a normal search on :book/author:"

(fact
  (datomic/select book-ds :book/author)
  => #{{:book {:name "The Magicians", :author "Lev Grossman"}}})

"Using the abridged schema, an exception is thrown:"

(fact
  (datomic/select book-ds :book/author :schema name-schema)
  => (throws))


[[:section {:title ":pull"}]]

"A connection is created using the schema:"

(def schema-store
   {:book   {:name    [{:required true
                        :fulltext true}]
             :author  [{:fulltext true}]}
    :inventory {:count [{:type :long}]
                :book    [{:type :ref
                           :ref  {:ns :book}}]
                :store   [{:type :ref
                           :ref  {:ns :store
                                  :rval :inventory}}]}
    :store  {:name    [{:required true
                        :fulltext true}]}})

(def store-ds (datomic/connect!
               {:uri "datomic:mem://datomic-schema-store"
                :schema schema-store
                :options {:install-schema true
                          :reset-db true}}))

"A store is created with inventory:"

(datomic/insert! store-ds
             {:store {:name "Happy Books"
                      :inventory [{:count 10
                                    :book {:name "The Magicians"
                                           :author "Lev Grossman"}}
                                  {:count 8
                                   :book {:name "The Color of Magic"
                                          :author "Terry Pratchett"}}]}})

[[:subsection {:title "defaults"}]]

"The default behaviour for `:pull` is to grab all attributes from an entity that are not references:"

(fact
  (datomic/select store-ds :store)
  => #{{:store {:name "Happy Books"}}})

[[:subsection {:title ":checked and :unchecked"}]]

"However, this can be customised by using `:checked` and `:unchecked`"

(fact
  (datomic/select store-ds :store
              :pull {:store {:name :unchecked
                             :inventory :checked}})
  => #{{:store {:inventory #{{:count 8} {:count 10}}}}})

[[:subsection {:title ":id"}]]

"The pull model can be nested and allows very quick customisation of data. Using `:id` restricts refs to return the internal id:"

(fact
  (datomic/select store-ds :store
              :pull {:store {:inventory {:book :id}}}
              :first)
  => (contains-in {:store {:name "Happy Books",
                           :inventory #{{:count 8, :book number?}
                                        {:count 10, :book number?}}}}))

[[:subsection {:title "forward walk"}]]

"Using `{}` for defining the `:pull` model for a ref is the same as using `:checked`"

(fact
  (datomic/select store-ds {:store/name "Happy Books"}
              :pull {:store {:inventory {:book {}}}})
  => #{{:store {:name "Happy Books",
                 :inventory #{{:count 10
                               :book {:name "The Magicians"
                                      :author "Lev Grossman"}}
                              {:count 8
                               :book {:name "The Color of Magic"
                                      :author "Terry Pratchett"}}}}}})

[[:subsection {:title "reverse walk"}]]

"`:pull` also works for reverse lookups, having the ability to walk the schema in the opposite direction:"

(fact
  (datomic/select store-ds {:book/name '(?fulltext "Magic")}
              :pull {:book {:inventories {:store :checked}}})
  => #{{:book {:name "The Color of Magic",
               :author "Terry Pratchett",
               :inventories #{{:count 8,
                               :store {:name "Happy Books"}}}}}})

[[:subsection {:title ":yield"}]]

"A special option `:yield` is available for recursive references. To show this in action, a different datastore is required:"

(def schema-nodes
   {:node   {:name   [{:required true}]
             :next   [{:type :ref
                       :ref {:ns :node
                             :rval :previous}}]}})

(def node-ds (datomic/connect!
              {:uri "datomic:mem://datomic-schema-nodes"
               :schema schema-nodes
               :options {:reset-db true
                         :install-schema true}}))

"A set of maps are inserted:"

(datomic/insert! node-ds {:node {:name "A"
                             :next {:name "B"
                                    :next {:name "C"
                                           :next {:name "D"}}}}})


"We can now perform selection:"

(fact
  (datomic/select node-ds {:node/name "B"})
  => #{{:node {:name "B"}}})

"when `:yield` is flagged on a ref, the `:pull` model yields to what was previously defined:"

(fact
  (datomic/select node-ds {:node/name "B"}
              :pull {:node {:next :yield}})
  => #{{:node {:name "B", :next {:name "C", :next {:name "D"}}}}})

[[:subsection {:title "recursion"}]]

"The option is great for arbitrarily linked data. Additionally, the data model can be walked both forwards and backwards"

(fact
  (datomic/select node-ds {:node/name "D"}
              :pull {:node {:previous :yield}})
  => #{{:node {:name "D", :previous #{{:name "C", :previous #{{:name "B", :previous #{{:name "A"}}}}}}}}})

"A mix of `:yield` and `:checked` makes for some interesting datastructures"

(fact
  (datomic/select node-ds {:node/name "C"}
              :pull {:node {:next :yield
                            :previous :checked}})
  => #{{:node {:name "C", :next {:name "D",
                                 :previous #{{:name "C"}}},
               :previous #{{:name "B"}}}}})

[[:section {:title ":access"}]]

"`:access` provides restrictions on input and an outline on how data should be returned. The following is how it is typically used:"

(fact
  (datomic/select node-ds {:node {:name "C"}}
              :access {:node :checked})
  => #{{:node {:name "C"}}})

[[:subsection {:title "restrictions"}]]

"When the selector oversteps what is deemed acceptable, an exception is thrown:"

(fact
  (datomic/select node-ds {:node {:next {:name "C"}}}
              :access {:node :checked})
  => (throws))

"The model is able to follow the data and the returned values:"

(fact
  (datomic/select node-ds {:node {:next {:name "C"}}}
              :access {:node {:next :checked}})
  => #{{:node {:name "B", :next {:name "C"}}}})

"Reverse lookups are also supported"

(fact
  (datomic/select node-ds {:node {:previous {:name "C"}}}
              :access {:node {:previous :checked}})
  => #{{:node {:name "D", :previous #{{:name "C"}}}}})

[[:subsection {:title "combination"}]]

"`:access` and `:pull` can be used to work together to limit data:"

(fact
  (datomic/select node-ds {:node {:next {:name "C"}}}
              :access {:node {:next :checked}}
              :pull   {:node :checked})
  => #{{:node {:name "B"}}})

"or to expand the data that is returned:"

(fact
  (datomic/select node-ds {:node {:next {:name "C"}}}
              :access {:node {:next :checked}}
              :pull   {:node {:next {:next :checked}}})
  => #{{:node {:name "B",
               :next {:name "C",
                      :next {:name "D"}}}}})
