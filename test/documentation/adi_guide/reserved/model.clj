(ns documentation.adi-guide.reserved.model
  (:use midje.sweet)
  (:require [adi.core :as adi]))


[[:section {:title ":pull"}]]

"A connection is created using the schema:"

(def schema-bookstore
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

(def store-ds (adi/connect! "datomic:mem://adi-guide-schema-bookstore" schema-bookstore true true))

"A store is created with inventory:"

(adi/insert! store-ds
             {:store {:name "Happy Books"
                      :inventory #{{:count 10
                                    :book {:name "The Magicians"
                                           :author "Lev Grossman"}}
                                   {:count 8
                                    :book {:name "The Color of Magic"
                                           :author "Terry Pratchett"}}}}})

[[:subsection {:title "defaults"}]]

"The default behaviour for `:pull` is to grab all attributes from an entity that are not references:"

(fact
  (adi/select store-ds :store)
  => #{{:store {:name "Happy Books"}}})

[[:subsection {:title ":checked and :unchecked"}]]

"However, this can be customised by using `:checked` and `:unchecked`"

(fact
  (adi/select store-ds :store
              :pull {:store {:name :unchecked
                             :inventory :checked}})
  => #{{:store {:inventory #{{:count 8} {:count 10}}}}})

[[:subsection {:title ":id"}]]

"The pull model can be nested and allows very quick customisation of data. Using `:id` restricts refs to return the internal id:"

(fact
  (adi/select store-ds :store
              :pull {:store {:inventory {:book :id}}})
  => #{{:store {:name "Happy Books",
                :inventory #{{:count 10, :book 17592186045418}}}}})

[[:subsection {:title "forward walk"}]]

"Using `{}` for defining the `:pull` model for a ref is the same as using `:checked`"

(fact
  (adi/select store-ds {:store/name "Happy Books"}
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
  (adi/select store-ds {:book/name '(?fulltext "Magic")}
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

(def node-ds (adi/connect! (adi/connect! "datomic:mem://adi-guide-schema-nodes" schema-nodes true true)))

"A set of maps are inserted:"

(adi/insert! node-ds {:node {:name "A"
                             :next {:name "B"
                                    :next {:name "C"
                                           :next {:name "D"}}}}})


"We can now perform selection:"

(fact
  (adi/select node-ds {:node/name "B"})
  => #{{:node {:name "B"}}})

"when `:yield` is flagged on a ref, the `:pull` model yields to what was previously defined:"

(fact
  (adi/select node-ds {:node/name "B"}
              :pull {:node {:next :yield}})
  => #{{:node {:name "B", :next {:name "C", :next {:name "D"}}}}})

[[:subsection {:title "recursion"}]]

"The option is great for arbitrarily linked data. Additionally, the data model can be walked both forwards and backwards"

(fact
  (adi/select node-ds {:node/name "D"}
              :pull {:node {:previous :yield}})
  => #{{:node {:name "D", :previous #{{:name "C", :previous #{{:name "B", :previous #{{:name "A"}}}}}}}}})

"A mix of `:yield` and `:checked` makes for some interesting datastructures"

(fact
  (adi/select node-ds {:node/name "C"}
              :pull {:node {:next :yield
                            :previous :checked}})
  => #{{:node {:name "C", :next {:name "D",
                                 :previous #{{:name "C"}}},
               :previous #{{:name "B"}}}}})

[[:section {:title ":access"}]]

[[:section {:title ":model"}]]

[[:section {:title ":profiles"}]]

"To be Done"
