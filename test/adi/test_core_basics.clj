(ns adi.test-core-basics
 (:use midje.sweet
       adi.utils
       adi.schema
       hara.common
       hara.checkers
       adi.api.schema)
 (:require [datomic.api :as d]
           [adi.core :as adi]))

(def ^:dynamic *ds* nil)

(future-fact "Options allowed and disallowed on unnested key fields"
    (do "Should NOT allow :required option in a single key field"
      (adi/datastore "datomic:mem://adi-core-test-basics"
                     {:id   [{:type :long :required true}]}
                     true true) nil)
  => (throws Exception)

  (do "Should NOT allow :default option in a single key field"
      (adi/datastore "datomic:mem://adi-core-test-basics"
                     {:id   [{:type :long :default  true}]}
                     true true) nil)
  => (throws Exception)

  (do "Allows the :restrict option in a single key field"
      (adi/datastore "datomic:mem://adi-core-test-basics"
                     {:id   [{:type :long :restrict #{1 2 3 4 5}}]}
                     true true) nil)
  => nil)

(fact ":restrict should work as expected on nested and unnested keys"
  (def ds (adi/datastore "datomic:mem://adi-core-test-basics"
                         {:id   [{:type :long :restrict #{1 2 3 4 5}}]
                          :account {:id   [{:type     :long
                                            :restrict #(#{1 2 3 4 5} %)}]}}
                         true true))
  (fact "Unnested Keys"
    (adi/insert! ds {:id 0}) => (throws Exception)
    (adi/insert! ds {:id 6}) => (throws Exception)
    (do (adi/insert! ds {:id 3})
        (adi/select ds :id) => (one-of (contains {:id 3}))))

  (fact "Nested Keys" (adi/insert! ds {:account {:id 0}}) => (throws Exception)
    (adi/insert! ds {:account/id 6}) => (throws Exception)
    (do (adi/insert! ds {:account/id 3})
        (adi/select ds :account/id) => (one-of (contains {:account {:id 3}}))
        (adi/select ds :account) => (throws Exception))))


(fact ":required works with nested keys"
  (def ^:dynamic *ds*
    (adi/datastore "datomic:mem://adi-core-test-basics"
                   {:account {:id   [{:type     :long
                                      :required true}]
                              :name [{:type :string}]}}
                   true true))

  (schema-required-keys *ds* :account)
  => #{:account/id}

  (fact "Adding a record without the namespace will not do anything"
      (adi/insert! *ds* {})
      (adi/select *ds* {:account/id 0})
    => '())

  (fact "Adding a record with the :account namespace will throw an exception"
      (adi/insert! *ds* {:account {}})) => (throws Exception)

  (fact "Adding a record with an unknown key will throw an exception."
      (adi/insert! *ds* {:account {:UNKNOWN "hello"}})) => (throws Exception)

  (fact "Adding a record without a required key will throw an exception."
    (adi/insert! *ds* {:account {:name "hello"}})) => (throws Exception)

  (fact "Adding a record with a required key will result in a normal operation"
    (adi/insert! *ds* {:account {:id 0}})
    (adi/select *ds* {:account/id 0})
    => (one-of (contains-in {:account {:id 0}}))
    (adi/select *ds* :account)
    => (one-of (contains-in {:account {:id 0}}))
    ))

(fact ":required behaviour with multiple nested keys"
  (def ^:dynamic *ds*
    (adi/datastore "datomic:mem://adi-core-test-basics"
                   {:account {:name [{}]
                              :icq  {:id    [{:type     :long
                                              :required true}]
                                     :label  [{}]}
                              }}
                   true true))

  (fact "Adding a record without the namespace will not do anything"
      (adi/insert! *ds* {})
      (adi/select *ds* {:account/icq/id 0})
    => '())

  (fact "Adding a record with the :account namespace will be fine (not in :account/icq)"
    (do (adi/insert! *ds* {:account {}}) nil)
    => nil)

  (fact "Adding a nested record in the account ns will be fine (not in :account/icq)"
    (do (adi/insert! *ds* {:account {:name "hello"}}) nil)
    => nil)

  (fact "Adding a nested record without a required key will result in a exception"
    (adi/insert! *ds* {:account {:icq/label "late"}})
    => (throws Exception))

  (fact "Adding a nested record with the required key will be fine."
    (adi/insert! *ds* {:account {:icq/id 0}})
    (adi/select *ds* {:account/icq/id 0})
    => (one-of (contains-in {:account {:icq {:id 0}}}))))

(fact ":required behaviour with multiple nested keys"
  (def ^:dynamic *ds*
    (adi/datastore "datomic:mem://adi-core-test-basics"
                   {:account {:name [{:required true}]
                              :icq  {:id    [{:type     :long}]}}}
                   true true))

  (fact "Adding a record without the namespace will not do anything"
      (adi/insert! *ds* {})
      (adi/select *ds* :account/name)
    => '())

  (fact "Adding a record with the :account namespace will throw an exception"
    (adi/insert! *ds* {:account {}}) => (throws Exception))

  (fact "Adding the required nested record will be fine"
    (do (adi/insert! *ds* {:account {:name "hello"}}) nil)
    => nil)

  (fact "Adding a nested record without a required key will throw exception"
    (adi/insert! *ds* {:account {:icq/label "late"}})
    => (throws Exception)))


(fact ":default works with nested keys"
  (def ^:dynamic *ds*
    (adi/datastore "datomic:mem://adi-core-test-basics"
                   {:account {:id   [{:type     :long
                                      :default  0}]
                              :name [{:type :string}]}
                    :user  {:id   [{:type     :long}]}}
                   true true))

  (fact "Adding a record without the namespace will not do anything"
    (adi/insert! *ds* {})
    (adi/select *ds* {:account {:id 0}})
    => '())

  (fact "Adding a record outside of the namespace will not do anything"
    (adi/insert! *ds* {:user/id 0})
    (adi/select *ds* {:account {:id 0}})
    => '())

  (fact "Adding a record with the :account namespace will throw an exception"
    (adi/insert! *ds* {:account {}})
    (adi/select *ds* :account/id)
    => (one-of (contains-in {:account {:id 0}})))

  (fact "Adding a record without a required key will throw an exception."
    (adi/insert! *ds* {:account {:name "hello"}})
    (adi/select *ds* :account/id)
    => (two-of (contains-in {:account {:id 0}}))))

(fact ":default works with nested keys"
  (def ^:dynamic *ds*
    (adi/datastore "datomic:mem://adi-core-test-basics"
                   {:account {:name [{:type :string}]
                              :icq {:id   [{:type :long :default  0}]}}}
                   true true))

  (fact "Adding a record without the namespace will not do anything"
    (adi/insert! *ds* {})
    (adi/select *ds* {:account/icq/id 0})
    => '())

  (fact "Adding a record outside of the namespace will not do anything"
    (adi/insert! *ds* {:account/name "hello"})
    (adi/select *ds* {:account/icq/id 0})
    => '()))

;; Testing required and default
(fact "Adding a record on default"
  (def ^:dynamic *ds*
  (adi/datastore "datomic:mem://adi-core-test-basics"
               {:account {:id   [{:type :long
                                  :required true
                                  :default 0}]
                          :name [{:type :string}]}}
               true true))

  (fact "Adding a record without the namespace will not do anything "
    (adi/insert! *ds* {})
    (adi/select *ds* {:account/id 0})
    => '())
  (do
    (adi/insert! *ds* {:account {:name "hello"}})
    (adi/select *ds* {:account/id 0})
    => (one-of (contains-in {:account {:id 0 :name "hello"}})))

  (do ""
    (adi/insert! *ds* {:account {}})
    (adi/select *ds* :account/id 0)
    => (two-of (contains-in {:account {:id 0}}))))
