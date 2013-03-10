(ns adi.core.test-01-simple
  (:use midje.sweet
        adi.utils
        adi.checkers)
  (:require [adi.core :as adi]))

(def uri "datomic:mem://adi.core.test-01-simple")
(def schema
        {:name      [{:type        :string}]
         :likes     [{:type        :keyword
                      :cardinality :many}]
         :dislikes  [{:type        :keyword
                      :cardinality :many}]})

(def data [{:name "adam"
            :likes #{:ice-cream :pizza :soft-drink}
            :dislikes #{:eggs :fish}}
           {:name "bob"
            :likes #{:fish :eggs :pizza}
            :dislikes #{:broccoli :brussel-sprouts}}
           {:name "chris"
            :likes #{:butter :ice-cream :broccoli}
            :dislikes #{:eggs :fish :meat}}
           {:name "dave"
            :likes #{:broccoli :brussel-sprouts :ice-cream
                     :pizza :soft-drink :eggs :fish}}])

;; Setup
(def ds (adi/datastore uri schema true true))
(adi/insert! ds data)

(fact "querying using the datomic interface"
  (adi/select ds {:#/not {:name #{"adam"  "chris"}}})
  => (every-checker (has-length 2)
                    (results-contain :name #{"bob" "dave"}))

  (adi/select ds {:likes :butter})
  => (every-checker (has-length 1)
                    (results-contain :name "chris"))

  (adi/select ds {:likes :butter})
  => (results-contain {:name "chris"})

  (adi/select ds {:likes #{:pizza :broccoli}}  )
  => (every-checker (has-length 1)
                    (results-contain {:name "dave"}))

  (adi/select ds {:#/not {:name "dave"}})
  => (results-contain :name #{"adam" "bob" "chris"})

  (adi/select ds {:#/not {:likes #{:pizza :broccoli}}})
  => (results-contain :name #{"adam" "bob" "chris" "dave" }))



(fact "select- queries... the negatives do not work properly"
  (adi/select ds '[:find ?e :where
                   [?e :likes ?v]
                   [((fn [v] (or (not= v :pizza) (not= v :broccoli))) ?v)]
                   ;;[?i :likes :pizza]
                   ;;[?i :likes :broccoli]
                   ;;[(not= ?i ?e)]
                   ]
              #{})
  => (every-checker (has-length 4)
                    (results-contain :name #{"adam" "bob" "chris" "dave" }))

  (adi/select ds '[:find ?e :where
                   [?e :name _]
                   [?e1 :likes :pizza]
                   [?e2 :likes :broccoli]
                   [(= ?e1 ?e2)]
                   [(not= ?e2 ?e)]]
              #{})
  => (results-contain :name #{"adam" "bob" "dave" "chris"})

  (adi/select ds  '[:find ?e :where
                    [?e :name _]
                    [?e1 :likes ?i1]
                    [(not= ?i1 :pizza)]
                    [?e2 :likes ?i2]
                    [(not= ?i1 :broccoli)]
                    ((not= ?e1 ?e2))
                    ((not= ?e1 ?e))]
              #{})
  => (results-contain :name #{"adam" "bob" "dave" "chris"}))
