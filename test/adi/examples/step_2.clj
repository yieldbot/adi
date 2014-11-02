(ns adi.examples.step-2
  (:use midje.sweet)
  (:require [adi.core :as adi]
            [adi.core.select :as select]
            [adi.test.checkers :refer :all]
            [datomic.api :as datomic]))

[[:section {:title "Step Two"}]]

[[:subsection {:title "Enums"}]]

"There is an intrinsic concept of enums in datomic, seen in the website's [schema documentation](http://docs.datomic.com/schema.html).
`adi` just takes this explicitly and incorporates it into the schema. We see that there is a new entry for the schema - the `account/type`
attribute which is of type :enum:"

(def schema-2
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

"Some comments regarding this particular definition compared to the one in Step One:

  1. Note that instead of using `:account/<attr>` to specify attributes in a flat hashmap, we can just nest the
attributes under the `:account` namespace. This allows for much better readability and saves a couple of characters.
  * The `:enum` type is a special `:ref` type that is defined [here](http://docs.datomic.com/schema.html#sec-3). The
library provides it easy to install and manage them. We put them under a common namespace (`:account.type`) and give
them allowed values `#{:admin :free :paid}`"


(facts
  [[:subsection {:title "Updating"}]]

  "Lets connect to a brand new database and insert some data. Note the different ways of nesting data. There is
a correspondence between a nested hashmap and a flat hashmap having keys representing data-paths. `adi` takes
advantage of this correspondence to give allow users more semantic freedom of how to represent their data:"

  (def ds (adi/connect! "datomic:mem://adi-examples-step-2" schema-2 true true))

  (adi/insert! ds {:account {:user "adi1"
                             :password "hello1"}
                   :account/type :paid})
  (adi/insert! ds [{:account {:password "hello2"
                              :type :account.type/admin}
                    :account/user "adi2"}
                   {:account {:user "adi3"
                              :credits 1000}
                    :account/password "hello3"}])

  "Lets take a look at all the `:admin` accounts:"

  (adi/select ds {:account/type :admin})
  => #{{:account {:user "adi2", :password "hello2", :credits 0, :type :admin}}}

  "We can make `adi1` into an :admin and then do another listing:"

  (adi/update! ds {:account/user "adi1"} {:account/type :admin})
  (adi/select ds {:account/type :admin})
  => #{{:account {:user "adi1", :password "hello1", :credits 0, :type :admin}}
       {:account {:user "adi2", :password "hello2", :credits 0, :type :admin}}}

  "If we attempt to add an value of `:account.type/<value>` that is not listed, an exception will be thrown:"

  (adi/insert! ds {:account {:user "adi4"
                             :password "hello4"
                             :type :vip}})
  => throws

  [[:subsection {:title "Selection"}]]

  "There are many ways of selecting data. We have already seen the basics:"

  (adi/select ds {:account/credits 1000} :first)
  => {:account {:user "adi3", :password "hello3", :credits 1000, :type :free}}

  "Adding a `:pull` model will filter out selection options:"

  (adi/select ds {:account/type :free} :first
              :pull {:account {:credits :unchecked
                                 :type :unchecked}})
  => {:account {:user "adi3", :password "hello3"}}

  "Another feature is that a list can be used to input an expression. In the examples below, the `'(> ? 10)` predicate
  acts as the `adi` equivalent of using an actual predicate `#(> % 10)`. If there is no `?`, it is
  assumed that the first argument is `?`.  Note that all the three queries below give the same results:"

  (adi/select ds {:account/credits '(> 10)} :first)
  => {:account {:user "adi3", :password "hello3", :credits 1000, :type :free}}

  (adi/select ds {:account/credits '(> ? 10)} :first)
  => {:account {:user "adi3", :password "hello3", :credits 1000, :type :free}}

  (adi/select ds {:account/credits '(< 10 ?)} :first)
  => {:account {:user "adi3", :password "hello3", :credits 1000, :type :free}})


(facts
  [[:subsection {:title "Java"}]]

  "Java expressions can also be used because these functions are executed at the transactor end:"

  (adi/select ds {:account/user '(.contains "2")} :first)
  => {:account {:user "adi2", :password "hello2", :credits 0, :type :admin}}

  (adi/select ds {:account/user '(.contains ? "2")} :first)
  => {:account {:user "adi2", :password "hello2", :credits 0, :type :admin}}

  (adi/select ds {:account/user '(.contains "adi222" ?)} :first)
  => {:account {:user "adi2", :password "hello2", :credits 0, :type :admin}})
