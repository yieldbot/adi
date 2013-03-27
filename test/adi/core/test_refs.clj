(ns adi.core.test-refs
 (:use midje.sweet
       adi.utils
       adi.schema
       adi.core
       adi.checkers)
 (:require [datomic.api :as d]
           [adi.api :as aa]
           [adi.core :as adi]))

(def ^:dynamic *ds*
  (datastore  "datomic:mem://adi-core-test-refs"
              {:account {:user   [{:type       :string}]
                         :email  [{:type       :ref
                                   :ref-ns     :email}]}
               :email   {:name   [{:type       :string}]
                         :type   [{:type       :keyword
                                   :keyword-ns :email.type}]}}
              true true))


(insert! [{:account/user "user1"
           :account/email/name "user1@testing.com"
           :account/email/type :main}
          {:account/user "user2"
           :account/email/name "user2@testing.com"
           :account/email/type :main}
          {:account/user "user3"
           :account/email/name "user3@testing.com"
           :account/email/type :home}
          {:account/user "user4"
           :account/email/name "user4@testing.com"
           :account/email/type :home}
          {:account/user "user5"
           :account/email/name "user5@testing.com"
           :account/email/type :work}
          {:account/user "user6"
           :account/email/name "user6@testing.com"
           :account/email/type :work}]
         *ds*)

(defn has-tree [m1]
  (fn [x]
    (empty? (diff-in m1 x))))

(fact
  (adi/select-first {:account/user "user1"} *ds* :ref-set #{:account/email})
  => (has-tree {:account {:user   "user1"
                          :email  {:type   :email.type/main
                                   :name   "user1@testing.com"}}})

  (adi/select-first {:account/user "user1"} *ds*)
  => (has-tree {:account {:user "user1"}}))
