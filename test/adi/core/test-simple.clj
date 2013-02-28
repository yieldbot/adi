(ns adi.core.test-00-simple
  (:use midje.sweet
        adi.utils
        adi.checkers)
  (:require [adi.data :as ad]
            [adi.api :as aa]
            [adi.schema :as as]
            [adi.core :as adi]
            [datomic.api :as d]))

(def uri "datomic:mem://adi.core.test-00-simple")
(def schema
        {:name  [{:type    :string}]
         :age   [{:type    :long}]
         :here  [{:type    :boolean
                  :default true}]})

(def data [{:name "adam"  :age 50} {:name "adam"  :age 50} {:name "adam"}
                 {:name "bob"   :age 50} {:name "bob"   :age 80}
                 {:name "chris" :age 20} {:name "chris" :age 50} {:name "chris" :age 80}
                 {:name "dave"  :age 20} {:name "dave"}
                 {:name "eddie" :age 20} {:name "eddie" :age 80}
                 {:age 20} {:age 20} {:age 20} {:age 50} {:age 80} {:age 50}])

;; Setup
(def ds (adi/datastore uri schema true true))
(adi/insert! ds data)

;;(aa/select-ids-query {:name "chris" :age 20})

;;(aa/make-clauses (gen-dsym) {:name "chris" :age 20})

(fact "select-ids returns just ids"
  (adi/select-ids ds {:name "chris" :age 20})
  => (has-length 1))

(hash-set? #{1 2})

(fact "q-select queries"
  (adi/q-select ds #{} "[:find ?e :where [?e :age 20]]")
  => (has-length 6)

  (adi/q-select ds #{} '[:find ?e :where [?e :here _]])
  => (has-length 18)

  (adi/q-select ds #{} '[:find ?e :where [?e :name "chris"]])
  => (has-length 3)

  (adi/q-select ds #{} '[:find ?e :where
                         [?e :name "chris"]
                         [?e :age 20]])
  => (has-length 1))

(fact "select queries"
  (adi/select ds :here)
  ;;=> (adi/q-select ds #{} '[:find ?e :where [?e :here _]])
  => (has-length 18)

  (adi/select ds {:age 20})
  ;;=> (adi/q-select ds #{} '[:find ?e :where [?e :age 20]])
  => (has-length 6)

  (adi/select ds {:name "chris"})
  ;;=> (adi/q-select ds #{} '[:find ?e :where [?e :name "chris"]])
  => (has-length 3)

  (adi/select ds {:name "chris" :age 20})
  => (has-length 1)

  (adi/select ds {:name "adam" :age 20})
  => (has-length 0)

  (adi/select ds {:name "adam" :age 50})
  => (has-length 2)

  (adi/select ds #{{:name "chris"} {:name "bob"}})
  => (has-length 5)

  (adi/select ds #{{:name "chris" :age 20} {:name "bob"}})
  => (has-length 3)

    (adi/select ds #{{:name "eddie" :age 20} {:name "eddie" :age 80}})
  => (has-length 2)

  (let [query  #{{:name "chris"} {:name "bob"}}
        ids    (adi/select-ids ds query)]
    (fact (adi/select ds ids) => (adi/select ds query)))

  )
