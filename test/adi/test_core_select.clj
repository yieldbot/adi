(ns adi.test-core-select
 (:use midje.sweet
       adi.utils
       adi.schema
       hara.common
       hara.checkers)
 (:require [datomic.api :as d]
           [adi.core :as adi]
           [adi.api :as aa]))
(comment

  (require '[citrine.schema :as schema])

  (def *ds* (adi/datastore schema/url schema/all))

  ;;(d/delete-database schema/url)

  (adi/select *ds* :account)

  (first
   (d/q '[:find ?tx ?tx-time
          :in $ ?a
          :where [_ ?a ?v ?tx _]
          [?tx :db/txInstant ?tx-time]]
        (d/history (d/db (:conn *ds*)))
        :order.item/sku))

  (def t-accounts
    [13194139534314 #inst "2013-05-27T02:17:47.389-00:00"])
  (def t-categories
    [13194139538160 #inst "2013-05-27T02:17:52.893-00:00"])
  (def t-products
    [13194139540408 #inst "2013-05-27T02:18:39.868-00:00"])
  (def t-product-category
    [13194139543521 #inst "2013-05-27T02:18:43.893-00:00"])
  (def t-orders
    [13194139543557 #inst "2013-05-27T02:19:03.963-00:00"])
  (def t-orderitem-product
    [13194139547377 #inst "2013-05-27T02:19:13.708-00:00"])
  (def t-order-account
    [13194139548925 #inst "2013-05-27T02:20:04.876-00:00"])


  (aa/select (d/as-of (d/db (:conn *ds*)) (first t-accounts))
             :account
             *ds*)

  (aa/select (d/as-of (d/db (:conn *ds*)) (first t-accounts))
             :product
             *ds*)



  (select *ds* :account :at (first t-accounts) :first)
  (select *ds* :product :at (first t-products))



  (comment
    (adi/select-ids <ds> <query> :at <time>)
    (adi/select-entities <ds> <query> :at <time>)
    (adi/select <ds> <query> :at <time> :view [<ns1/field1> <ns1/field2>])
    (adi/select <ds> <query> :at <time> :view #{<ns1> <ns2})
    (adi/select <ds> <query> :at <time> :view {<ns1> {<field1> :hide
                                                      <field2> :show
                                                      <field3> :follow}}
                :first :hide-ids :show-data :follow-refs :show-refs)


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
    )
)
