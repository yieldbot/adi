(ns adi.examples.step-3
  (:use midje.sweet)
  (:require [adi.core :as adi]
            [adi.test.checkers :refer :all]
            [datomic.api :as datomic]))

[[:section {:title "Step Three"}]]

[[:subsection {:title "Refs"}]]

"The most significant feature that `adi` has implemented on top of datomic is the way that it
deals with `:ref` types. We add more attributes to our schema, this time, we include a `:book`
namespace:"

(def schema-3
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

(facts
  "Lets connect and print out the datastore:"

  (def ds (adi/connect! "datomic:mem://adi-example-step-3" schema-3 true true))

  (comment
    (println ds)
    ;; #adi{:connection #connection{1000 #inst "2014-10-29T03:28:33.478-00:00"},
    ;;        :schema #schema{:account {:books :&book<*>,
    ;;                                  :credits :long,
    ;;                                  :type :enum,
    ;;                                  :password :string,
    ;;                                  :user :string},
    ;;                        :book {:accounts :&account<*>,
    ;;                               :author :string,
    ;;                               :name :string}}}
    )

  "Note that the schema shows a couple of strange symbols - `:&book<*>` and `:&account<*>. These are symbols
  that say that `:account/book` is an attribute of type `:ref` that is pointing to the `:book` namespace.
  The <*> means that there is multiple cardinality associated with the attribute. Having done that, lets
  add some data to our datastore:"

  (adi/insert! ds {:account {:user "adi1" :password "hello1"}})
  (adi/insert! ds {:account {:user "adi2" :password "hello2"
                             :books #{{:name "The Count of Monte Cristo"
                                       :author "Alexander Dumas"}
                                      {:name "Tom Sawyer"
                                       :author "Mark Twain"}
                                      {:name "Les Miserables"
                                       :author "Victor Hugo"}}}})

  [[:subsection {:title "Walking"}]]

  "We start off by listing all the accounts:"

  (adi/select ds :account)
  => #{{:account {:user "adi1" :password "hello1" :credits 0 :type :free}}
       {:account {:user "adi2" :password "hello2" :credits 0 :type :free}}}

  "We can use `:return` to specify a model to return."

  (adi/select ds :account
              :return {:account {:books :checked}})
  => #{{:account {:credits 0 :type :free :password "hello1" :user "adi1"}}
       {:account {:books #{{:author "Mark Twain" :name "Tom Sawyer"}
                           {:author "Victor Hugo" :name "Les Miserables"}
                           {:author "Alexander Dumas" :name "The Count of Monte Cristo"}}
                  :credits 0 :type :free :password "hello2" :user "adi2"}}}

  "Using :id instead of :checked will return book ids. This is very useful for copying references"

  (adi/select ds :account
              :return {:account {:books :id}})
  => #{{:account {:credits 0, :type :free, :password "hello1", :user "adi1"}}
       {:account {:credits 0, :type :free, :password "hello2", :user "adi2"
                  :books #{17592186045425 17592186045426 17592186045424}}}}

  [[:subsection {:title "Data Access"}]]

  "We will now use the `:access` option instead of `:return`. Essentially, they do the same thing
  except tha `:access` limits the data model on the way in and on the way out, whilst `:return` limits
  the data model on the way out only. We can look at a case where both are the same. In the case below, using
  :access or :return does not matter and will yield the same result:"

  (adi/select ds :account
              :access {:account {:books {:author :unchecked}
                                 :credits :unchecked
                                 :type :unchecked}})
  => #{{:account {:user "adi1" :password "hello1"}}
       {:account {:user "adi2" :password "hello2"
                  :books #{{:name "Tom Sawyer"}
                           {:name "The Count of Monte Cristo"}
                           {:name "Les Miserables"}}}}}

  "### Return

   In the case where they differ, this is the result of using the `:return` option:"

  (adi/select ds {:account {:books/author "Victor Hugo"}}
              :first
              :return {:account {:books {:author :unchecked}
                                 :credits :unchecked
                                 :type :unchecked}})
  => {:account {:books #{{:name "Tom Sawyer"}
                         {:name "The Count of Monte Cristo"}
                         {:name "Les Miserables"}}
                :password "hello2" :user "adi2"}}


  "### Access

  And this is the result of using the `:access` option. The difference is that because
  the `:account/book/author` path is `:unchecked`, the operation raises an exception to
  say that a query like this is not allowed:"

  (adi/select ds {:account {:books/author "Victor Hugo"}}
              :first
              :access {:account {:books {:author :unchecked}
                                 :credits :unchecked
                                 :type :unchecked}})

  => (raises-issue {:key-path [:account :books :author]
                    :nsv [:book :author]
                    :data "Victor Hugo"
                    :not-allowed true})

  "### Combination

  To fix this problem limiting searches to the `:account/books` path we can use both `:access`
  and `:return` models for fine-tuning control over how our data is accessed:"
  (adi/select ds {:account {:books/author "Victor Hugo"}}
              :first
              :access {:account {:books :checked}}
              :return {:account {:books {:author :unchecked}
                                 :credits :unchecked
                                 :type :unchecked}})
  => {:account {:books #{{:name "Tom Sawyer"}
                         {:name "The Count of Monte Cristo"}
                         {:name "Les Miserables"}}
                :password "hello2" :user "adi2"}}


  [[:subsection {:title "Pointers"}]]

  "Entity `:db/id` keys are essentially pointers to data. We can add references to other enities just by
  using copying these `:db/id` keys around. Instead of returning data, we can use the :return-ids option to
  return a set of entity ids associated with the search:"

  (adi/select ds :account :get :ids)
  => #{17592186045421 17592186045423}

  "Having this is super nice because we can just use these like pointers. We can add `The Book and the Sword`
  to our datastore and link them to both our user accounts straight away:"

  (let [account-ids (adi/select ds :account :get :ids)]
    (adi/insert! ds [{:book {:name "The Book and the Sword"
                             :author "Louis Cha"
                             :accounts account-ids}}]))

  "Now a search on all the books that `adi` has yields one result:"

  (adi/select ds {:book {:accounts/user "adi1"}})
  => #{{:book {:name "The Book and the Sword" :author "Louis Cha"}}}

  [[:subsection {:title "Playground"}]]

  "Lets do a couple more inserts and selects just to show off some different features"

  "###Path Reversal

  Inserts can use both forward and backward references. In this case, we are adding a book with a bunch
  of accounts:"

  (adi/insert! ds {:book {:name "Charlie and the Chocolate Factory"
                          :author "Roald Dahl"
                          :accounts #{{:user "adi3" :password "hello3" :credits 100}
                                      {:user "adi4" :password "hello4" :credits 500}
                                      {:user "adi5" :password "hello5" :credits 500}}}})

  (adi/select ds {:account {:books/author "Roald Dahl"}}
              :return {:account {:password :unchecked
                                 :credits :unchecked
                                 :type :unchecked}})
  => #{{:account {:user "adi3"}}
       {:account {:user "adi4"}}
       {:account {:user "adi5"}} }


  "###Fulltext searches

  Fulltext searches are avaliable on schema attributes defined with :fulltext true:"

  (adi/select ds {:book/author '(?fulltext "Louis")}
              :return {:book {:accounts :checked}} :first)
  => {:book {:author "Louis Cha" :name "The Book and the Sword"
             :accounts #{{:credits 0 :type :free :password "hello2" :user "adi2"}
                         {:credits 0 :type :free :password "hello1" :user "adi1"}}}})
