(ns adi.examples.step-1
  [:use midje.sweet]
  (:require [adi.core :as adi]
            [adi.test.checkers :refer :all]))

[[:section {:title "Step One"}]]

[[:subsection {:title "Schema"}]]

"A simple `adi` schema is constructed as follows:"

(def schema-1
  {:account/user     [{:type :string      ;; (1)
                       :cardinality :one
                       :unique :value     ;; (2)
                       :required true}]   ;; (3)
   :account/password [{:required true     ;; (1) (3)
                       :restrict ["password needs an integer to be in the string"
                                  #(re-find #"\d" %)]}] ;; (4)
   :account/credits  [{:type :long
                       :default 0}]})

"There are a couple of things to note about this particular schema:

  1. We specified the `:type` for `:account/user` to be `:string` and `:cardinality` to be `:one`. However, because these are default options, we can optionally leave them out for `:account/password`.
  * We want the value of `:account/user` to be unique.
  * We require that both `:account/user` and `:account/password` to be present on insertion.
  * We are checking that `:account/password` contains at least one number
  * We set the default amount of `:account/credits` to be `0`
"

(facts
  [[:subsection {:title "Connection"}]]

  "So having a basic schema, an adi datastore can be constructed. Note that we are connecting
  to a standard datomic in memory store url. The philosophy of `adi` is that it should work
  seamlessly with datomic. From experience, `adi` should provide enough expressiveness to do
  about **95%** of all the `CRUD` in an application. The user can perform more complicated or optimised
  queries by dropping back into the `datomic` api if needed:"

  (def ds (adi/connect! "datomic:mem://adi-examples-step-1" schema-1 true true))

  "The last two arguments are flags for `reset?` and `install-schema?`. To blow away the old datastare
  and construct a brand new one, set both flags as `true`. If connecting to an already existing datastore
  set both flags as `false` (by default) otherwise all data will be lost."

  [[:subsection {:title "Writing"}]]

  "Once a datastore has been established, lets add some records. Note that multiple records can
  be added by using a vector of multiple records:"

  (adi/insert! ds {:account {:user "angela"   :password "hello1"}})

  (adi/insert! ds [{:account {:user "billy"    :password "pass1"}}
                   {:account {:user "carl"     :password "pass1"}}])


  [[:subsection {:title "Reading"}]]

  "Now that there is data, we can then do a search for the record using `select`."

  (adi/select ds {:account {:password "pass1"}})
  => #{{:account {:user "billy", :password "pass1", :credits 0}}
       {:account {:user "carl", :password "pass1", :credits 0}}}

  "We can use the `:first` option to pull the first entry of the set. This is useful when you know
  that there is only one result from the query. We can also use the `:ids` option to add the entity `:db/id`
  field onto the data:"

  (adi/select ds {:account {:user "angela"}} :first true :ids true)
  => {:db {:id 17592186045418}, :account {:user "angela", :password "hello1", :credits 0}}

  "For option keys such as `:first` and `:ids`, we can just use the keyword itself, so this invocation works as well"

  (adi/select ds {:account {:user "angela"}} :first :ids)
  => {:db {:id 17592186045418}, :account {:user "angela", :password "hello1", :credits 0}}

  [[:subsection {:title "Restrictions"}]]

  "We now learn what we can't do and how the schema helps in keeping our data regular. Lets try to
  add in some incomplete data:"

  (adi/insert! ds {:account {:credits 10}})
  => (throws Exception "The following keys are required: #{:account/password :account/user}")

  (adi/insert! ds {:account {:user "adi"}})
  => (throws Exception "The following keys are required: #{:account/password}")

  "As can be seen by the examples above, because we have set the `:required` attribute to true,
  both `:account/password` and `:account/user` are needed before the data is considered valid
  input. There is also an additional `:restrict` attribute for the password field and this takes
  care of additional validation on inputs. We see below that the insert fails because the password
  has to contain at least one integer:"

  (adi/insert! ds {:account {:user "adi" :password "hello"}})
  => (raises-issue {:failed-restriction true})

  "Any data that lies outside of the schema will also cause the insert to fail. This can be disabled
  through using access models but for now, we will let the insert fail on insertion of an `:account/type` field"

  (adi/insert! ds {:account {:user "adi" :password "hello1" :type :vip}})
  => (raises-issue {:nsv [:account :type] :no-schema true})

  "Field uniqueness is something that datomic supports natively. We can trigger a failed
  transaction by attempting to insert another user named `billy` into the system:"

  (adi/insert! {:account {:user "billy" :password "pass2"}})
  => throws

  [[:subsection {:title "Time Travel"}]]

  "A native feature of datomic allows users to access the state of the database at any point in time.
  This is also supported by `adi`. We can use `transactions` to list all the transactions involving
  a particular attribute:"

  (adi/transactions ds :account/user)
  => [1001 1003]

  "So we can see that there are two transactions involving the :account/user attribute. Lets narrow in
  and find the transaction number involving the user `angela`. Note that we inserted `angela` before `billy`
  `carl`:"

  (adi/transactions ds :account/user "angela")
  => [1001]

  "We can also see when the transaction has occured. This of course will be different for everyone:"

  (adi/transaction-time ds 1001)  ;; -> #inst "2014-10-29T00:17:09.961-00:00"
  => #(instance? java.util.Date %)

  "We can use the `:at` option to input either a transaction number or the time. It can be seen that
  at 1001, only a single record is in the datastore, whilst at 1003, all three records have be added."

  (adi/select ds :account :at 1001)
  => #{{:account {:user "angela", :password "hello1", :credits 0}}}

  (adi/select ds :account :at 1003)
  => #{{:account {:user "angela", :password "hello1", :credits 0}}
       {:account {:user "billy", :password "pass1", :credits 0}}
       {:account {:user "carl", :password "pass1", :credits 0}}}

  "There is nicer format for `adi` such that the schema as well as the connection object is
  prettied up to display an abriged schema as well as the time and id of the last transaction."

  (comment
    (println ds)

    ;; #adi{:connection #connection{1003 #inst "2014-10-29T00:31:28.206-00:00"},
    ;;      :schema #schema{:account {:credits :long, :password :string, :user :string}}}
    ))
