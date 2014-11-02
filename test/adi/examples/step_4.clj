(ns adi.examples.step-4
  (:use midje.sweet)
  (:require [adi.core :as adi]
            [adi.test.checkers :refer :all]
            [datomic.api :as datomic]))

[[:chapter {:title "Example - Bookstore"}]]

"Lets go ahead and model a network of bookstores around the world. We apply
the knowledge of `:ref` and `:enum` types from previous tutorial to good use:"

[[:section {:title "Definition and Setup"}]]

"The schema for our bookstore model can be seen in `Figure {{schema-4}}`. It is a rather simplistic
model. The two main concepts being a `Book` and a `Store`. They are connected together
through an `Inventory`, which keeps how many books there are in a store:"

[[:image {:tag "schema-4" :title "Schema Diagram"
          :src "example4.png"}]]

"The actual schema code is defined as follows:"

(def schema-4
 {:book   {:name    [{:required true
                      :fulltext true}]
           :author  [{:fulltext true}]}

  :inventory {:count [{:type :long}]
              :cover [{:type :enum
                       :enum {:ns :cover.type
                              :values #{:hard :soft}}}]
              :book    [{:type :ref
                         :ref  {:ns :book}}]
              :store   [{:type :ref
                         :ref  {:ns :store}}]}
  :store  {:name    [{:required true
                      :fulltext true}]
           :address [{:type :ref
                      :ref  {:ns :address}}]}

  :address {:country [{:required true}]}})


(facts

  "Once again, the adi datastore is created and the seed stores created"

  (def ds (adi/connect! "datomic:mem://adi-example-step-4" schema-4 true true))

  (adi/insert! ds [{:address {:country "USA"
                              :stores #{{:name "Canyon Books"}
                                        {:name "Capital Books"}}}}
                   {:db/id [[:AUS]]
                    :address {:country "Australia"}}
                   {:store {:address [[:AUS]]
           :name "Koala Books"}}])

  "We can see our stores from Australia:"

  (adi/select ds {:store {:address/country "Australia"}})
  => #{{:store {:name "Koala Books"}}}

  "As well as the USA:"

  (adi/select ds {:store {:address/country "USA"}})
  => #{{:store {:name "Canyon Books"}} {:store {:name "Capital Books"}}}

  [[:section {:title "Updating Data"}]]

  "We can now start adding some books to our Australian store:"

  (adi/update! ds {:store {:address/country "Australia"}}
               {:store/inventories #{{:count 10
                                      :cover :hard
                                      :book {:name "The Count of Monte Cristo"
                                             :author "Alexander Dumas"}}
                                     {:count 5
                                      :cover :hard
                                      :book {:name "Tom Sawyer"
                                             :author "Mark Twain"}}
                                     {:count 3
                                      :cover :soft
                                      :book {:name "Les Miserables"
                                             :author "Victor Hugo"}}}})

  "Query for the inventory containing a book in our network starting with `\"Les\"`. Notice the
  use of the `:first` keyword, this will return a single result as opposed to a set of results:"

  (adi/select ds {:inventory {:book/name '(?fulltext "Les")}} :first)
  => {:inventory {:count 3, :cover :soft}}

  "`update-in!` is a very useful function as it can be used to set values across references.
  In this case, we set the inventory count of `\"Tom Sawyer\"` in `\"Koala Books\"` to `4`:"

  (adi/update-in! ds {:store/name "Koala Books"}
                  [:store/inventories {:book/name "Tom Sawyer"}]
                  {:count 4})

  "We can look at the inventory of a specific book and store by specifying more search keys:"

  (adi/select ds {:inventory {:book/name "Tom Sawyer"
                              :store/name "Koala Books"}} :first)
  => {:inventory {:count 4, :cover :hard}}

  "We can also return all the books in the network, along with their inventories and stores.
  This is done by providing an `:access <MODEL>` pair of args. In this case, we specify that
  linked refs in `:inventories` and `:stores` are also returned:"

  (adi/select ds :book :access {:book {:inventories {:store :checked}}})
  => #{{:book {:author "Mark Twain" :name "Tom Sawyer"
               :inventories #{{:store {:name "Koala Books"}
                               :count 4 :cover :hard}}}}
       {:book {:author "Victor Hugo" :name "Les Miserables"
               :inventories #{{:store {:name "Koala Books"}
                               :count 3  :cover :soft}}}}
       {:book {:author "Alexander Dumas" :name "The Count of Monte Cristo"
               :inventories #{{:store {:name "Koala Books"}
                               :count 10 :cover :hard}}}}}

  [[:section {:title "Different Semantics, Same Result"}]]

  "Lets test out some other functions, starting with `retract!`. Retract takes a set of
   keys to remove from an entity:"

  (adi/retract! ds {:inventory {:book/name "Tom Sawyer"
                                :store/name "Koala Books"}}
                #{:inventory/cover})
  (comment
    (adi/retract! ds {:inventory/book '_}
                  #{:inventory/book/name} :raw)

    (adi/retract-in! ds :book
                     [:book/inventories '_]
                     #{:count})

    (adi/update-in! ds :book
                    [:book/inventories '_]
                    {:count 4})

    (adi/delete-in! ds :book
                    [:book/inventories '_]
                    )

    )


  "The result of the retract is that the cover information is missing when we do a search:"
  (adi/select ds {:inventory {:book/name "Tom Sawyer"
                              :store/name "Koala Books"}} :first)
  => {:inventory {:count 4}}
)
