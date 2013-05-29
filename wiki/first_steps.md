# Adi First Steps


### Step 1

Lets show off some things we can do with adi. We start off by defining a user account and gradually add more features as we go along. 

Fire up nrepl/emacs/vim and load `adi.core`.
```clojure
(require '[adi.core :as adi])
```
We first have to define a `geni`, which is a template for our data:
```clojure            
(def geni-1
 {:account/user     [{:type :string      ;; (1)
                      :cardinality :one  
                      :unique :value     ;; (2)
                      :required true}]   ;; (3)
  :account/password [{:required true     ;; (1) (3)
                      :restrict ["password needs an integer to be in the string" 
                                   #(re-find #"\d" %)] ;; (4)
  :account/credits  [{:type :long     
                      :default 0}]}]}})   ;; (5)
```
There are a couple of things to note about our definitions the entry.
  1. We specified the `:type` for `:account/user` to be `:string` and `:cardinality` to be `:one`. However, because these are default options, we can optionally leave them out for `:account/password`.
  2. We want the value of `:account/user` to be unique.
  3. We require that both `:account/user` and `:account/password` to be present on insertion.
  4. We are checking that `:account/password` contains at least one number
  5. We set the default amount of credits in the account to be 0
  
Now, we construct a datastore. 
```clojure            
(def ds (adi/datastore "datomic:mem://example-1" geni-1 true true))
```
The parameters are:
   uri   The uri of the datomic database to connect to
   geni  The previously defined data template 
   install?  - Optional flag (if true, will install the `geni` into the database) 
   recreate? - Optional flag (if true will delete and then create the database)

So lets attempt to add some data. We'll do this via trial and error:
```clojure
(adi/insert! ds {:account {:credits 10}})
;; => (throws Exception "The following keys are required: #{:account/user :account/password}")

(adi/insert! ds {:account {:user "adi"}})
;;=> (throws Exception "The following keys are required: #{:account/password}")

(adi/insert! ds {:account {:user "adi" :password "hello"}})
;;=> (throws Exception "The value hello does not meet the restriction: password needs an integer to be in the string")

(adi/insert! ds {:account {:user "adi" :password "hello1" :type :vip}})
;;=> (throws Exception "(:type :vip) not in schema definition")

(adi/insert! ds {:account {:user "adi" :password "hello1"}})
;;=> Finally, No Errors! Our data is finally installed. Lets do one more:

(adi/insert! ds {:account {:user "adi" :password "hello2" :credits 10}})
;;=> (throws Exception "ExceptionInfo :transact/bad-data Unique conflict: :account/user, value: adi already held")

(adi/insert! ds {:account {:user "adi2" :password "hello2" :credits 10}})
;;=> Okay, another record inserted!
```

We can now have a play with the data:
```clojure
(adi/select ds :account)
;;=> ({:db {:id 17592186045418}, :account {:user "adi", :password "hello1", :credits 0}}
;;    {:db {:id 17592186045420}, :account {:user "adi2", :password "hello2", :credits 10}})

(adi/select ds :account :hide-ids) ;; We can hide ids for ease of view
;;=> ({:account {:user "adi", :password "hello1", :credits 0}}
;;    {:account {:user "adi2", :password "hello2", :credits 10}})

;; We can also look at transactions when for a particular attribute has been changed
(adi/transactions ds :account/user)
;;=> (1001 1003)

;; We can also look at specific values of the changed attributes
(adi/transactions ds :account/user "adi")
;;=> (1001)

;; We can also select our data at the point of the actual transaction
(adi/select ds :account :at 1001 :hide-ids)
;;=> ({:account {:user "adi", :password "hello1", :credits 0}})
```

### Step 2
So lets 

```clojure
(def geni-2           ;; (1)
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
```
Comments
 1. Note that instead of using `:account/<attr>` way in Step 1 to specify attributes, we can just nest the attributes under the account `:account` namespace. This allows for much better readability.
   
 2. The enum type is a special :ref type that is defined here http://docs.datomic.com/schema.html#sec-3. adi makes it easy to install and manage them. We put them under a common namespace (:account.type) and give them values #{:admin :free :paid}


Lets explore enums a little bit more by going under the covers of adi. Using datomic,
we can see that the enums have been installed as datomic refs:

```clojure
(require '[datomic.api :as d])
(def ds (adi/datastore "datomic:mem://example-2" geni-2 true true))

(d/q '[:find ?x :where
       [?x :db/ident :account.type/free]]
     (d/db (:conn ds)))
;;=> #{[17592186045417]}

(d/q '[:find ?x :where
       [?x :db/ident :account.type/paid]]
     (d/db (:conn ds)))
;;=> #{[17592186045418]}
```
Lets insert some data. We can insert multiple records in one transaction:
```clojure
(adi/insert! ds [{:account {:user "adi1"          ;; (1)
                            :password "hello1"}
                  :account/type :paid}            ;; (2)
                 {:account {:password "hello2" :type
                            :account.type/admin}  ;; (2)
                  :account/user "adi2"}
                 {:account {:user "adi3"
                            :credits 1000}
                  :account/password "hello3"}])
```
Comments
  1. data can be formatted arbitrarily as long as the `/` is consistent with the level of map nesting. This is a design decision to allow maximal readability.
  2. enums can be specified fully `:account.type/<value>` or as just `:<value>`, also they will always be outputted as the full version.


We can play with the data again:
```clojure
(adi/select ds :account :hide-ids)
;;=> ({:account {:user "adi1", :password "hello1", :credits 0, :type :account.type/paid}}
;;    {:account {:user "adi2", :password "hello2", :credits 0, :type :account.type/admin}}
;;    {:account {:user "adi3", :password "hello3", :credits 1000, :type :account.type/free}})

(adi/transactions ds :account/user)
;;=> (1004) 

(adi/select ds :account :at 1003)
;;=> '()
 
(adi/insert! ds {:account {:user "adi4"
                           :password "hello4"
                           :type :vip}})
;;=>  (throws Exception "The value :vip does not meet the restriction: #{:free :paid :admin}")
```