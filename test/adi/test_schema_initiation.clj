(ns adi.test-schema-initiation
  (:use midje.sweet
        adi.schema
        hara.common
        hara.hash-map)
  (:require [datomic.api :as d]
            [adi.core :as adi]
            [adi.api :as aa]))


(def lab-schema2 {:sample {:id [{:type :string
                                 :unique :ident}]}})

(def uri* "datomic:mem://adi-test-schema-initiation")
(def fgeni (infer-fgeni lab-schema2))

(fact
  (adi/datastore uri* lab-schema2)
  => (throws Exception)

  (aa/install-schema (:conn ds) fgeni)
  => (throws Exception)

  (emit-schema fgeni)
  => (throws Exception)

  (emit-single-schema (fgeni :sample/id))
  => (throws Exception)

  (verify fgeni)
  => (throws Exception))

(comment
  (defn needs-an-integer [s]
    (re-find #"\d" s))

  (def geni-1
    {:account {:user     [{:required true
                           :unique :value}]
               :password [{:required true
                           :restrict ["password needs an integer to be in the string"
                                      #(re-find #"\d" %)]}]
               :credits  [{:type :long
                           :default 0}]}})

  (def ds (adi/datastore "datomic:mem://example-1" geni-1 true true))

  (adi/insert! ds {:account {:credits 10}})
  ;; => (throws Exception "The following keys are required: #{:account/user :account/password}")

  (adi/insert! ds {:account {:user "adi"}})
  ;;=> (throws Exception "The following keys are required: #{:account/password}")

  (adi/insert! ds {:account {:user "adi" :password "hello"}})
  ;;=> (throws Exception "The value hello does not meet the restriction: password needs an integer to be in the string")

  (adi/insert! ds {:account {:user "adi" :password "hello1" :type :vip}})
  ;;=> (throws Exception "(:type :vip) not in schema definition")

  (adi/insert! ds {:account {:user "adi" :password "hello1"}})
  ;;=> Finally, No Errors! Our data is installed, Lets Do Another one

  (adi/insert! ds {:account {:user "adi" :password "hello2" :credits 10}})
  ;;=> (throws Exception "ExceptionInfo :transact/bad-data Unique conflict: :account/user, value: adi already held")

  (adi/insert! ds {:account {:user "adi2" :password "hello2" :credits 10}})
  ;;=> Okay, inserted

  (adi/select ds :account)
  ;;=> ({:db {:id 17592186045418}, :account {:user "adi", :password "hello1", :credits 0}}
  ;;    {:db {:id 17592186045420}, :account {:user "adi2", :password "hello2", :credits 10}})

  (adi/select ds :account :hide-ids) ;; We can hide ids
  ;;=> ({:account {:user "adi", :password "hello1", :credits 0}}
  ;;    {:account {:user "adi2", :password "hello2", :credits 10}})

  (adi/select ds {:account/user "adi"} :first :hide-ids)
  ;;=> {:account {:user "adi", :password "hello1", :credits 0}}

  (adi/transactions ds :account/user)
  ;;=> (1001 1003)

  (adi/transactions ds :account/user "adi")
  ;;=> (1001)

  (adi/select ds :account :at 1001 :hide-ids)
  ;;=> ({:account {:user "adi", :password "hello1", :credits 0}})


  (def geni-2
    {:account {:user     [{:required true
                           :unique :value}]
               :password [{:required true
                           :restrict ["password needs an integer to be in the string"
                                      #(re-find #"\d" %)]}]
               :credits  [{:type :long
                           :default 0}]
               :type     [{:type :enum
                           :default :free
                           :enum {:ns :account.type
                                  :values #{:admin :free :paid}}}]}})

  (def ds (adi/datastore "datomic:mem://example-2" geni-2 true true))

  ;; Going a little bit under the covers,
  ;; we can see that the enums are actually
  ;; installed as datomic refs

  (d/q '[:find ?x :where
         [?x :db/ident :account.type/free]]
       (d/db (:conn ds)))
  ;;=> #{[17592186045417]}

  (d/q '[:find ?x :where
         [?x :db/ident :account.type/paid]]
       (d/db (:conn ds)))
  ;;=> #{[17592186045418]}

  ;; Lets insert some data:
  (adi/insert! ds [{:account {:user "adi1"
                              :password "hello1"}
                    :account/type :paid}

                   {:account {:password "hello2" :type
                              :account.type/admin}
                    :account/user "adi2"}

                   {:account {:user "adi3"
                              :credits 1000}
                    :account/password "hello3"}])

  (adi/select ds :account :hide-ids)
  ;;=> ({:account {:user "adi1", :password "hello1", :credits 0, :type :account.type/paid}}
  ;;    {:account {:user "adi2", :password "hello2", :credits 0, :type :account.type/admin}}
  ;;    {:account {:user "adi3", :password "hello3", :credits 1000, :type :account.type/free}})

  (adi/select ds {:account/type :admin} :first :hide-ids)
  ;;=> {:account {:user "adi2", :password "hello2", :credits 0, :type :account.type/admin}}

  (adi/select ds {:account/credits 1000} :first :hide-ids)
  ;;=> {:account {:user "adi3", :password "hello3", :credits 1000, :type :account.type/free}}

  (adi/select ds {:account/credits '(> 10)} :first :hide-ids)
  ;;=> {:account {:user "adi3", :password "hello3", :credits 1000, :type :account.type/free}}

  (adi/select ds {:account/credits '(> ? 10)} :first :hide-ids)
  ;;=> {:account {:user "adi3", :password "hello3", :credits 1000, :type :account.type/free}}

  (adi/select ds {:account/credits '(< 10 ?)} :first :hide-ids)
  ;;=> {:account {:user "adi3", :password "hello3", :credits 1000, :type :account.type/free}}

  (adi/select ds {:account/user '(.contains "2")} :first :hide-ids)
  ;;=> {:account {:user "adi2", :password "hello2", :credits 0, :type :account.type/admin}}

  (adi/select ds {:account/user '(.contains ? "2")} :first :hide-ids)
  ;;=> {:account {:user "adi2", :password "hello2", :credits 0, :type :account.type/admin}}

  (adi/select ds {:account/user '(.contains "adi222" ?)} :first :hide-ids)
  ;;=> {:account {:user "adi2", :password "hello2", :credits 0, :type :account.type/admin}}

  (adi/transactions ds :account/user)
  ;;=> (1004)

  (adi/insert! ds {:account {:user "adi4"
                             :password "hello4"
                             :type :vip}})
  ;;=>  (throws Exception "The value :vip does not meet the restriction: #{:free :paid :admin}")



  (def geni-3
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

  (def ds (adi/datastore "datomic:mem://example-3" geni-3 true true))


  (adi/insert! ds
               [{:account {:user "adi1" :password "hello1"}}
                {:account {:user "adi2" :password "hello2"
                           :books #{{:name "The Count of Monte Cristo"
                                     :author "Alexander Dumas"}
                                    {:name "Tom Sawyer"
                                     :author "Mark Twain"}
                                    {:name "Les Misérables"
                                     :author "Victor Hugo"}}}}])

  (adi/select ds :account :view #{:account/books} :hide-ids)
  ;;=> ({:account {:user "adi1", :password "hello1", :credits 0, :type :account.type/free}}
  ;;    {:account {:user "adi2", :password "hello2", :credits 0,
  ;;           :books #{{:author "Alexander Dumas", :name "The Count of Monte Cristo"}
  ;;                   {:author "Mark Twain", :name "Tom Sawyer"}
  ;;                   {:author "Victor Hugo", :name "Les Misérables"}},
  ;;           :type :account.type/free}})

  (def users (adi/select-ids ds :account))

  (adi/insert! ds [{:book {:name "Charlie and the Chocolate Factory"
                           :author "Roald Dahl"
                           :accounts #{{:user "adi3" :password "hello3" :credits 100}
                                       {:user "adi4" :password "hello4" :credits 500}
                                       {:user "adi5" :password "hello5" :credits 500}}}}
                   {:book {:name "The Book and the Sword"
                           :author "Louis Cha"
                           :accounts users}}])

  (adi/select ds {:account/user "adi1"} :view #{:account/books} :first :hide-ids)
  ;;=> {:account {:user "adi1", :password "hello1", :credits 0,
  ;;           :books #{{:author "Louis Cha", :name "The Book and the Sword"}}, :type :account.type/free}}

  (adi/select ds {:account/user "adi1"} :first :hide-ids :view {:account/books :show})
  ;;=> {:account {:user "adi1", :password "hello1", :credits 0,
  ;;           :books #{{:+ {:db {:id 17592186045431}}}}, :type :account.type/free}}

  (adi/select ds {:account/user "adi1"} :first :hide-ids :view {:account/books :follow})
  ;;=> {:account {:user "adi1", :password "hello1", :credits 0,
  ;;              :books #{{:author "Louis Cha", :name "The Book and the Sword"}}, :type :account.type/free}}

  (adi/select ds {:account/user "adi1"} :first :hide-ids :view {:account/books :follow
                                                                :account/user :hide
                                                                :account/password :hide
                                                                :account/credits :hide
                                                                :account/type :hide})
  ;;=> {:account {:books #{{:author "Louis Cha", :name "The Book and the Sword"}}}}


  (adi/select ds {:account/user "adi3"} :view #{:account/books} :first :hide-ids)
  ;;=> {:account {:user "adi3", :password "hello3", :credits 100,
  ;;           :books #{{:author "Roald Dahl", :name "Charlie and the Chocolate Factory"}}, :type :account.type/free}}

  (adi/select ds {:book/author '(.startsWith ? "Mark")} :hide-ids :first)
  ;;=> {:book {:author "Mark Twain", :name "Tom Sawyer"}}

  (adi/select ds {:book/author '(?fulltext "Louis")} :view #{:book/accounts} :hide-ids :first)
  ;;=>  {:book {:author "Louis Cha", :name "The Book and the Sword",
  ;;        :accounts #{{:user "adi2", :password "hello2", :credits 0, :type :account.type/free}
  ;;                   {:user "adi1", :password "hello1", :credits 0, :type :account.type/free}}}}

  (adi/select ds {:book/accounts/user "adi2"} :hide-ids)
  ;;=> ({:book {:author "Alexander Dumas", :name "The Count of Monte Cristo"}}
  ;;  {:book {:author "Mark Twain", :name "Tom Sawyer"}}
  ;;  {:book {:author "Victor Hugo", :name "Les Misérables"}}
  ;;  {:book {:author "Louis Cha", :name "The Book and the Sword"}})


  (adi/select ds {:account/books/name '(.contains ? "the")} :hide-ids)
  ;;=> ({:account {:user "adi1", :password "hello1", :credits 0, :type :account.type/free}}
  ;;    {:account {:user "adi2", :password "hello2", :credits 0, :type :account.type/free}}
  ;;    {:account {:user "adi3", :password "hello3", :credits 100, :type :account.type/free}}
  ;;    {:account {:user "adi4", :password "hello4", :credits 500, :type :account.type/free}}
  ;;    {:account {:user "adi5", :password "hello5", :credits 500, :type :account.type/free}})
















  (fact
    (adi/insert! ds {:account {:user "adi"}})
    => (throws Exception "The following keys are required: #{:account/password}")

    (adi/insert! ds {:account {:user "adi"
                               :password "hello"}})
    => (throws Exception "The value hello does not meet the restriction: password needs an integer to be in the string")

    (adi/insert! ds {:account {:user "anne"
                               :password "hello1"
                               :follows {:user "betty"
                                         :password "hello2"}}})

    (adi/select ds {:account/follower/user "anne"})


    ({:account {:user "betty", :password "hello2", :type :account.type/free}}
     {:account {:user "anne", :password "hello1", :follows #{}, :type :account.type/free}}
     {:account {:user "adi", :password "hello1", :type :account.type/paid}}
     {:account {:user "chris1", :password "hello1", :type :account.type/free}})
    (adi/select ds {:account/user "adi"} :first :hide-ids)
    => {:account {:user "adi", :password "hello1", :type :account.type/free}}

    (adi/insert! ds {:account {:user "adi" :password "hello1" :type :vip}})
    => (throws Exception "The value :vip does not meet the restriction: #{:free :paid :admin}")

    (adi/insert! ds {:account {:user "adi" :password "hello1" :type :paid}})
    => (throws Exception "ExceptionInfo :transact/bad-data Unique conflict: :account/user, value: adi already held")
    )


  (adi/insert! ds {:account {:user "chris1"
                             :password "hello1"}})

  (adi/select ds :account :hide-ids)
  (adi/select ds :account)


  (def geni-3
    {:account {:user     [{:required true
                           :unique :value}]
               :password [{:required true
                           :restrict ["password needs an integer to be in the string"
                                      #(re-find #"\d" %)]}]
               :type     [{:type :enum
                           :default :free
                           :enum {:ns :account.type
                                  :values #{:admin :free :paid}}}]
               :books    [{:type :ref
                           :cardinality :many
                           :ref  {:ns :book}}]

               :follows  [{:type :ref
                           :cardinality :many
                           :ref  {:ns   :account
                                  :rval :follower}}]}
     :book   {:name    [{:required true}]
              :author  [{}]}})
)
