(ns adi.test-core-basics-enum
 (:use midje.sweet
       adi.utils
       adi.schema
       hara.common
       hara.checkers)
 (:require [datomic.api :as d]
           [adi.core :as adi]))

(def ds (adi/datastore "datomic:mem://adi-core-test-basics"
                         {:likes   [{:type :enum
                                     :enum {:ns :likes.food
                                            :values #{:broccolli :carrot :apples}}}]}
                         true true))

(def all-props
  (let [data (d/q '[:find ?ident ?type ?cardinality ?e :where
                        [?e :db/ident ?ident]
                        [?e :db/valueType ?t]
                        [?t :db/ident ?type]
                        [?e :db/cardinality ?c]
                        [?c :db/ident ?cardinality]]
                  (d/db (ds :conn)))]
    (zipmap (map first data) data)))

(def all-keys
  (let [data (d/q '[:find ?ident ?e :where
                    [?e :db/ident ?ident]]
                  (d/db (ds :conn)))]
    (-> (zipmap (map first data) data)
        (dissoc (keys all-props)))))

(emit-schema
 (infer-fgeni {:likes   [{:type :enum
                          :enum {:ns :likes
                                 :values #{:broccolli :carrot :apples}}}]}))

(fact "Options allowed and disallowed on unnested key fields"

  (adi/insert! ds {:likes :apples})
  (adi/insert! ds {:likes :likes.food/apples})
  (adi/update! ds 17592186045421 {:likes :gravy})
  => (throws Exception)
  (suppress (adi/update! ds 17592186045421 {:likes :likes.food/carrot})
            )

  (adi/select ds :likes)
  (first (adi/select-ids ds :likes))17592186045421
  )
