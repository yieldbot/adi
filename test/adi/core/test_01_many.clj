(ns adi.core.test-01-many
  (:use midje.sweet
        adi.utils
        adi.checkers)
  (:require [adi.data :as ad]
            [adi.schema :as as]
            [adi.core :as adi]
            [adi.api :as aa]
            [datomic.api :as d]))

(def uri "datomic:mem://adi.core.test-01-many")
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

  (adi/select ds {:+/not [[:name "adam"] [:name "chris"]]})
  => (every-checker (has-length 2)
                    (results-contain :name #{"bob" "dave"}))

  (adi/select ds #{{:likes :butter}})
  => (every-checker (has-length 1)
                    (results-contain :name "chris"))

  (adi/select ds #{{:likes :butter}})
  => (results-contain {:name "chris"})

  (adi/select ds [[:likes :pizza]
                  [:likes :broccoli]])
  => (every-checker (has-length 1)
                    (results-contain {:name "dave"}))

  (adi/select ds {:+/not [[:name "dave"]]})
  => (results-contain :name #{"adam" "bob" "chris"})

  (adi/select ds {:+/not [[:likes :pizza]
                          [:likes :broccoli]]}))



(fact "q-select"
  (adi/q-select ds #{} '[:find ?e :where
                         [?e :likes ?v]
                         [((fn [v] (or (not= v :pizza) (not= v :broccoli))) ?v)]
                         ;;[?i :likes :pizza]
                         ;;[?i :likes :broccoli]
                         ;;[(not= ?i ?e)]
                         ])
  => (every-checker (has-length 3)
                    (results-contain :name #{"adam" "bob" "chris"}))

  (adi/q-select ds #{} '[:find ?e :where
                         [?e :name _]
                         [?e1 :likes :pizza]
                         [?e2 :likes :broccoli]
                         [(= ?e1 ?e2)]
                         [(not= ?e2 ?e)]])
  => (results-contain :name #{"adam" "bob" "dave" "chris"})

  (adi/q-select ds #{} '[:find ?e :where
                         [?e :name _]
                         [?e1 :likes ?i1]
                         [(not= ?i1 :pizza)]
                         [?e2 :likes ?i2]
                         [(not= ?i1 :broccoli)]
                         ((not= ?e1 ?e2))
                         ((not= ?e1 ?e))])

)

(comment
  (aa/select-ids-query [[:likes :pizza]
                        [:likes :broccoli]
                        [:+/not      {:likes :fish}]
                        [:+/not-all  {:likes #{:broccoli :pizza}}]
                        [:+/fulltext {:name "stuff"}]
                        ])

  (aa/select-ids-query 9)
  (adi/select ds {:name "chris"}))
