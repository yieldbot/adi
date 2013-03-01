(ns adi.data.test-07-clauses
  (:use midje.sweet
        adi.utils
        adi.checkers)
  (:require [adi.data :as ad]))

(def category-map
  (flatten-keys
   {:category {:name         [{:type        :string}]
               :tags         [{:type        :string
                               :cardinality :many}]
               :image        [{:type        :ref
                               :ref-ns      :image}]
               :children     [{:type        :ref
                               :ref-ns      :category
                               :cardinality :many}]}
    :image     {:url         [{:type        :string}]
                :type        [{:type        :keyword}]}}))

(fact "clauses will make the datomic query"
  (-> {:#/sym '?e :category {:name "root" :tags #{"shop" "new"}}}
      (ad/process category-map {:use-sets true})
      (ad/characterise category-map {:generate-syms true})
      ad/clauses)
  => [['?e :category/tags "new"]
      ['?e :category/tags "shop"]
      ['?e :category/name "root"]])

(def ch-data-1
  (-> {:#/sym '?v
       :category {:name "root"
                  :image #{{:#/sym '?i1
                            :type :big}
                           {:#/sym '?i2
                            :type :small}}}}
      (ad/process category-map {:use-sets true})
      (ad/characterise category-map {:generate-syms true})))

(fact "clauses will automatically relate refs"
  (ad/clauses ch-data-1)
  => '[[?v :category/name "root"]
       [?v :category/image ?i2]
       [?v :category/image ?i1]
       [?i2 :image/type :small]
       [?i1 :image/type :big]]
  (ad/build-query ch-data-1 category-map))

(def ch-data-1a
  (-> {:#/sym '?e
       :category {:name "root"
                  :image #{{:type :big}
                           {:type :small}}}}
          (ad/process category-map {:use-sets true})
          (ad/characterise category-map {:generate-syms true
                                         :p-gen (ad/pretty-gen "r")})))

(fact "clauses will automatically relate refs"
  (ad/clauses ch-data-1a)
  => '[[?e :category/name "root"]
       [?e :category/image ?r2]
       [?e :category/image ?r1]
       [?r2 :image/type :small]
       [?r1 :image/type :big]]
  (ad/build-query ch-data-1 category-map))

(def ch-data-2
  (->
   {:#/sym '?e
    :#/not {:category/name "chris"
            :category/tags #{"happy" "sad"}}}
   (ad/process category-map {:use-sets true})
   (ad/characterise category-map {:generate-syms true})))

(fact
  (ad/clauses-not ch-data-2 category-map true)
  => '[[?e :category/tags ?ng1]
       [(not= ?ng1 "happy")]
       [?e :category/tags ?ng2]
       [(not= ?ng2 "sad")]
       [?e :category/name ?ng3]
       [(not= ?ng3 "chris")]])

(def ch-data-fulltext
  (->
   {:#/sym '?e
    :#/fulltext {:category/name "chris"
                 :category/tags #{"happy" "sad"}}}
   (ad/process category-map {:use-sets true})
   (ad/characterise category-map {:generate-syms true})))

(fact "fulltext clauses"
  (ad/clauses-fulltext ch-data-fulltext category-map true)
  => '[[(fulltext $ :category/tags "happy") [[?e ?ft1]]]
       [(fulltext $ :category/tags "sad") [[?e ?ft2]]]
       [(fulltext $ :category/name "chris") [[?e ?ft3]]]])


(def ch-data-total
  (-> {:#/sym '?e
       :category {:name "root" :tags #{"shop" "new"}}
       :#/not {:category/name "chris"
               :category/tags #{"happy" "sad"}}
       :#/fulltext {:category/name "chris"
                    :category/tags #{"happy" "sad"}}
       :#/q '[[?e :category/name "hello"]]}
      (ad/process category-map {:use-sets true})
      (ad/characterise category-map {:generate-syms true})))

(fact "a bunch of mixed clauses"
  (ad/build-query ch-data-total category-map true)
  => '[:find ?e :where
       [?e :category/tags "new"]
       [?e :category/tags "shop"]
       [?e :category/name "root"]
       [?e :category/tags ?ng1]
       [(not= ?ng1 "happy")]
       [?e :category/tags ?ng2]
       [(not= ?ng2 "sad")]
       [?e :category/name ?ng3]
       [(not= ?ng3 "chris")]
       [(fulltext $ :category/tags "happy") [[?e ?ft1]]]
       [(fulltext $ :category/tags "sad") [[?e ?ft2]]]
       [(fulltext $ :category/name "chris") [[?e ?ft3]]]
       [?e :category/name "hello"]])
