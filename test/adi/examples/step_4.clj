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

  "The result of the retract is that the cover information is missing when we do a search:"
  (adi/select ds {:inventory {:book/name "Tom Sawyer"
                              :store/name "Koala Books"}} :first)
  => {:inventory {:count 4}}

  "We can update the cover information using `update-in!`:"
  (adi/update-in! ds {:book/name "Tom Sawyer"}
                  [:book/inventories {:store/name "Koala Books"}]
                  {:cover :soft})
  (adi/select ds {:inventory {:book/name "Tom Sawyer"}} :first)
  => {:inventory {:count 4, :cover :soft}}

  "Oops. we made a mistake. The cover can also be changed using `update!`
  using slightly different semantics:"
  (adi/update! ds {:inventory {:book/name "Tom Sawyer"
                               :store/name "Koala Books"}}
               {:inventory/cover :hard})
  (adi/select ds {:inventory {:book/name "Tom Sawyer"}} :first)
  => {:inventory {:count 4, :cover :hard}}

  "The choice of using `update!` and  `update-in!` is purely in its communication:

  - `update!`: Find an inventory entity that has `:book/name` of `\"Tom Sawyer\"` and `:store/name` of `\"Koala Books\"` and change the value of `:inventory/cover` to `:soft`.
  - `update-in!`: Start off by finding a book with `:book/name` of `\"Tom Sawyer\"`. Then follow the entity accessible through `:book/inventories` which must be linked to a store with the name `\"Koala Books\"`. Then make the update of `:cover` to `:hard`."

  "Another way to reach the inventory entity is to start by finding the store, then following the `:store/stock` link. This time, we are changing the `:inventory/count` value to `3`:"
  (adi/update-in! ds {:store/name "Koala Books"}
                  [:store/inventories {:book/name "Tom Sawyer"}]
                  {:count 3})
  (adi/select ds {:inventory {:book/name "Tom Sawyer"}} :first)
  => {:inventory {:count 3, :cover :hard}}

  [[:section {:title "Expanding the Business"}]]

  "Lets replicate the stock at `Koala Books` to `Capital Books`. So basically, we are creating inventory
  records that have a link to the already existing books. It is very simple to do using adi:"

  (let [koala-inventories (adi/select ds {:inventory {:store/name "Koala Books"}}
                                      :return {:inventory {:book :id}})]
    (adi/update! ds {:store/name "Capital Books"}
                 {:store/inventories (set (map :inventory koala-inventories))}))

  "Now, lets look at where we can find `Tom Sawyer` in our stores:"

  (adi/select ds {:book/name "Tom Sawyer"} :return {:book {:inventories {:store :checked}}} :first)
  => {:book {:author "Mark Twain", :name "Tom Sawyer",
             :inventories #{{:cover :hard, :count 3, :store {:name "Koala Books"}}
                            {:cover :hard, :count 3, :store {:name "Capital Books"}}}}}

  "A fire burnt all the copies of the `Count of Monte Cristo` from `Koala Books`. Instead of
  setting the count to `0`, we can use `delete!` to get rid of the inventory entity entirely"

  (adi/delete! ds {:inventory {:store/name "Koala Books"
                               :book/name '(?fulltext "Count")}})

  "We see that now there is only one inventory record:"

  (adi/select ds {:book/name '(?fulltext "Count")}
              :return {:book {:inventories {:store :checked}}} :first)
  => {:book {:author "Alexander Dumas", :name "The Count of Monte Cristo",
             :inventories #{{:cover :hard, :count 10, :store {:name "Capital Books"}}}}}

  "The copies of `Tom Sawyer` exceeded expectations and sold out. We will delete the
  inventory record but this time using `delete-in!`:"

  (adi/delete-in! ds {:store/name "Capital Books"}
                  [:store/inventories {:book/name "Tom Sawyer"}])
  (adi/select ds {:book/name "Tom Sawyer"}
              :return {:book {:inventories {:store :checked}}} :first)
  => {:book {:author "Mark Twain", :name "Tom Sawyer",
             :inventories #{{:cover :hard, :count 3, :store {:name "Koala Books"}}}}}

  [[:section {:title "Moving Stock"}]]

  "`Capital Books` ran into financial problems and had to close down. All the stock was
  transfered to `Canyon Books`. In order to perform the whole lot in one transaction, the
  macro `sync->` is used:"

  (let [inventory-ids (adi/select ds {:inventory {:store/name "Capital Books"}}
                                  :return-ids)]
    (adi/sync-> ds
     (adi/update! {:store/name "Canyon Books"}
                  {:store/inventories inventory-ids})
     (adi/delete! {:store/name "Capital Books"})))

  "Now we can see that `Capital Books` is no more:"

  (adi/select ds :store :return {:store {:inventories {:cover :unchecked
                                                       :book {:author :unchecked}}}})
  => #{{:store {:name "Koala Books"
                :inventories #{{:book {:name "Les Miserables"} :count 3}
                               {:book {:name "Tom Sawyer"} :count 3}}}}
       {:store {:name "Canyon Books"
                :inventories #{{:book {:name "The Count of Monte Cristo"} :count 10}
                               {:book {:name "Les Miserables"} :count 3}}}}})
