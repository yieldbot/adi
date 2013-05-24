(ns adi.test-core-basics-enum
 (:use midje.sweet
       adi.utils
       adi.schema
       hara.common
       hara.checkers)
 (:require [datomic.api :as d]
           [adi.core :as adi]))

(def ds (adi/datastore
         "datomic:mem://adi-core-test-basics"
         {:likes
          [{:type :enum
            :enum {:ns :likes.food
                   :values #{:broccolli :carrot :apples}}}]}
         true true))

(def carrot-ident
  (first (adi/select-ids ds '[:find ?e :where
                              [?e :db/ident :likes.food/carrot]])))

(fact "Options allowed and disallowed on unnested key fields"
  (adi/insert! ds {:likes '_})
  => (throws Exception)

  (adi/insert! ds {:likes :likes.food/apples})
  (adi/select ds :likes)
  => (one-of (contains {:likes :likes.food/apples}))

  (def id (first (adi/select-ids ds :likes)))
  (do (adi/update! ds id {:likes :broccolli})
      (adi/select ds :likes))
  => (one-of (contains {:likes :likes.food/broccolli}))

  (do (adi/update! ds id {:likes carrot-ident})
      (adi/select ds :likes))
  => (one-of (contains {:likes :likes.food/carrot})))



(comment
  (emit-schema
   (infer-fgeni
    {:likes
     [{:type :enum
       :enum {:ns :likes
              :values #{:broccolli :carrot :apples}}}]})))
