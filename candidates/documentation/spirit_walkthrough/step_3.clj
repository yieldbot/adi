(ns documentation.spirit-walkthrough.step-3
  (:use hara.test)
  (:require [spirit.core :as spirit]
            [spirit.test.checkers :refer :all]
            [datomic.api :as datomic]))

[[:chapter {:title "Step Three"}]]

[[:section {:title "Refs"}]]

"The most significant feature that `spirit` has implemented on top of datomic is the way that it
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

  (def ds (spirit/connect! "datomic:mem://spirit-example-step-3" schema-3 true true))

  (comment
    (println ds)
    ;; #spirit{:connection #connection{1000 #inst "2014-10-29T03:28:33.478-00:00"},
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

  (spirit/insert! ds {:account {:user "spirit1" :password "hello1"}})
  (spirit/insert! ds {:account {:user "spirit2" :password "hello2"
                             :books #{{:name "The Count of Monte Cristo"
                                       :author "Alexander Dumas"}
                                      {:name "Tom Sawyer"
                                       :author "Mark Twain"}
                                      {:name "Les Miserables"
                                       :author "Victor Hugo"}}}})


  [[:section {:title "Walking"}]]

  "We start off by listing all the accounts:"

  (spirit/select ds :account)
  => #{{:account {:user "spirit1" :password "hello1" :credits 0 :type :free}}
       {:account {:user "spirit2" :password "hello2" :credits 0 :type :free}}}

  "We can use `:pull` to specify a model to pull."

  (spirit/select ds :account
              :pull {:account {:books :checked}})
  => #{{:account {:credits 0 :type :free :password "hello1" :user "spirit1"}}
       {:account {:books #{{:author "Mark Twain" :name "Tom Sawyer"}
                           {:author "Victor Hugo" :name "Les Miserables"}
                           {:author "Alexander Dumas" :name "The Count of Monte Cristo"}}
                  :credits 0 :type :free :password "hello2" :user "spirit2"}}}

  "Using :id instead of :checked will pull book ids. This is very useful for copying references"

  (spirit/select ds :account
              :pull {:account {:books :id}})
  => #{{:account {:credits 0, :type :free, :password "hello1", :user "spirit1"}}
       {:account {:credits 0, :type :free, :password "hello2", :user "spirit2"
                  :books #{17592186045425 17592186045426 17592186045424}}}}

  [[:section {:title "Data Access"}]]

  "We will now use the `:access` option instead of `:pull`. Essentially, they do the same thing
  except tha `:access` limits the data model on the way in and on the way out, whilst `:pull` limits
  the data model on the way out only. We can look at a case where both are the same. In the case below, using
  :access or :pull does not matter and will yield the same result:"

  (spirit/select ds :account
              :access {:account {:books {:author :unchecked}
                                 :credits :unchecked
                                 :type :unchecked}})
  => #{{:account {:user "spirit1" :password "hello1"}}
       {:account {:user "spirit2" :password "hello2"
                  :books #{{:name "Tom Sawyer"}
                           {:name "The Count of Monte Cristo"}
                           {:name "Les Miserables"}}}}}

  [[:subsection {:title "Return"}]]

  "In the case where they differ, this is the result of using the `:pull` option:"

  (spirit/select ds {:account {:books/author "Victor Hugo"}}
              :first
              :pull {:account {:books {:author :unchecked}
                                 :credits :unchecked
                                 :type :unchecked}})
  => {:account {:books #{{:name "Tom Sawyer"}
                         {:name "The Count of Monte Cristo"}
                         {:name "Les Miserables"}}
                :password "hello2" :user "spirit2"}}

  [[:subsection {:title "Access"}]]

  "This is the result of using the `:access` option. The difference is that because
  the `:account/book/author` path is `:unchecked`, the operation raises an exception to
  say that a query like this is not allowed:"

  (spirit/select ds {:account {:books/author "Victor Hugo"}}
              :first
              :access {:account {:books {:author :unchecked}
                                 :credits :unchecked
                                 :type :unchecked}})

  => (raises-issue {:key-path [:account :books :author]
                    :nsv [:book :author]
                    :data "Victor Hugo"
                    :not-allowed true})

  [[:subsection {:title "Combination"}]]

  "To fix this problem limiting searches to the `:account/books` path we can use both `:access`
  and `:pull` models for fine-tuning control over how our data is accessed:"
  (spirit/select ds {:account {:books/author "Victor Hugo"}}
              :first
              :access {:account {:books :checked}}
              :pull {:account {:books {:author :unchecked}
                                 :credits :unchecked
                                 :type :unchecked}})
  => {:account {:books #{{:name "Tom Sawyer"}
                         {:name "The Count of Monte Cristo"}
                         {:name "Les Miserables"}}
                :password "hello2" :user "spirit2"}}


  [[:section {:title "Pointers"}]]

  "Entity `:db/id` keys are essentially pointers to data. We can add references to other enities just by
  using copying these `:db/id` keys around. Instead of pulling data, we can use the :pull-ids option to
  pull a set of entity ids associated with the search:"

  (spirit/select ds :account :return :ids)
  => #{17592186045421 17592186045423}

  "Having this is super nice because we can just use these like pointers. We can add `The Book and the Sword`
  to our datastore and link them to both our user accounts straight away:"

  (let [account-ids (spirit/select ds :account :return :ids)]
    (spirit/insert! ds [{:book {:name "The Book and the Sword"
                             :author "Louis Cha"
                             :accounts account-ids}}]))

  "Now a search on all the books that `spirit` has yields one result:"

  (spirit/select ds {:book {:accounts/user "spirit1"}})
  => #{{:book {:name "The Book and the Sword" :author "Louis Cha"}}}

  [[:section {:title "Playground"}]]

  "Lets do a couple more inserts and selects just to show off some different features"

  [[:subsection {:title "Path Reversal"}]]

  "Inserts can use both forward and backward references. In this case, we are adding a book with a bunch
  of accounts:"

  (spirit/insert! ds {:book {:name "Charlie and the Chocolate Factory"
                          :author "Roald Dahl"
                          :accounts #{{:user "spirit3" :password "hello3" :credits 100}
                                      {:user "spirit4" :password "hello4" :credits 500}
                                      {:user "spirit5" :password "hello5" :credits 500}}}})

  (spirit/select ds {:account {:books/author "Roald Dahl"}}
              :pull {:account {:password :unchecked
                                 :credits :unchecked
                                 :type :unchecked}})
  => #{{:account {:user "spirit3"}}
       {:account {:user "spirit4"}}
       {:account {:user "spirit5"}} }

  [[:subsection {:title "Fulltext Searches"}]]

  "Fulltext searches are avaliable on schema attributes defined with :fulltext true:"

  (spirit/select ds {:book/author '(?fulltext "Louis")}
              :pull {:book {:accounts :checked}} :first)
  => {:book {:author "Louis Cha" :name "The Book and the Sword"
             :accounts #{{:credits 0 :type :free :password "hello2" :user "spirit2"}
                         {:credits 0 :type :free :password "hello1" :user "spirit1"}}}}


  [[:subsection {:title "Model Controlled Deletion"}]]

  "Deletes are controlled by models:"

  (spirit/delete-all! ds {:book/author "Roald Dahl"}
                   :access {:book {:accounts :checked}})
  (spirit/select ds :account)
  => #{{:account {:user "spirit2", :password "hello2", :credits 0, :type :free}}
       {:account {:user "spirit1", :password "hello1", :credits 0, :type :free}}}


  ;;(spirit/update-in)
  )
