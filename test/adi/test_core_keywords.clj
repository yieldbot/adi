(ns adi.test-core-keywords
 (:use midje.sweet
       adi.utils
       adi.schema
       hara.common
       hara.checkers)
 (:require [datomic.api :as d]
           [adi.core :as adi]))

(def ds nil)

(facts "Testing unnested keyword field"
  (def ds (adi/datastore "datomic:mem://adi-core-test-keywords"
                         {:likes   [{:type :keyword
                                     :keyword  {:ns   :likes}}]}
                         true true))

  (adi/insert! ds {:likes "oeuoeu"}) => (throws Exception)
  (do (adi/insert! ds {:likes :eggplant})

      (adi/select-view-val {:likes :eggplant})
      => #{:likes}
      (adi/select ds {:likes :eggplant})
      => (one-of (contains {:likes :likes/eggplant}))
      (adi/select ds {:likes :likes/eggplant})
      => (one-of (contains {:likes :likes/eggplant}))
      (adi/select ds '[:find ?x :where
                       [?x :likes _]])
      => (one-of (contains {:likes :likes/eggplant}))))

(facts
  (adi/select ds :likes)
  => (one-of (contains {:likes :likes/eggplant}))
  (adi/select ds {:likes '_})
  => (one-of (contains {:likes :likes/eggplant})))

(facts
  (def ds (adi/datastore "datomic:mem://adi-core-test-keywords"
                         {:likes   [{:type :keyword
                                     :keyword  {:ns   :likes}}]}
                         true true))

  (do (adi/insert! ds {:likes :likes/tomatos})

      (adi/select ds {:likes :likes/tomatos} :view {:likes :show})
      => (one-of (contains {:likes :likes/tomatos}))
      (adi/select ds '[:find ?x :where
                       [?x :likes _]])
      => (one-of (contains {:likes :likes/tomatos}))
      (adi/select ds :likes)
      => (one-of (contains {:likes :likes/tomatos}))
      (adi/select ds {:likes '_})
      => (one-of (contains {:likes :likes/tomatos}))))
