(ns adi.data.test-clauses
  (:use midje.sweet
        adi.utils
        adi.data
        adi.schema
        adi.checkers))

(def c1-geni
  (add-idents
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

(def c1-opts {:geni c1-geni
              :fgeni (flatten-all-keys c1-geni)
              :pretty-gen true})

;; Unit Testing







;; Function Testing

(fact "clauses-init will make the datomic query"
  (-> {:#/sym '?e :category {:name "root" :tags #{"shop" "new"}}}
      (process c1-geni {:sets-only? true})
      (characterise (flatten-all-keys c1-geni) {:generate-syms true})
      clauses-init set)
  => (set [['?e :category/tags "new"]
           ['?e :category/tags "shop"]
           ['?e :category/name "root"]]))

(def c1-data
  (-> {:#/sym '?v
       :category {:name "root"
                  :image #{{:#/sym '?i1
                            :type :big}
                           {:#/sym '?i2
                            :type :small}}}}
      (process c1-geni {:sets-only? true})
      (characterise (flatten-all-keys c1-geni) {:generate-syms true})))

(fact "clauses-init will automatically relate refs"
  (clauses-init c1-data)
  => '[[?v :category/name "root"]
       [?v :category/image ?i2]
       [?v :category/image ?i1]
       [?i2 :image/type :small]
       [?i1 :image/type :big]]
  (clauses c1-data c1-geni))

(def c2-data
  (-> {:#/sym '?e
       :category {:name "root"
                  :image #{{:type :big}
                           {:type :small}}}}
      (process c1-geni {:sets-only? true})
      (characterise (flatten-all-keys c1-geni) {:generate-syms true
                                                :sym-gen (clauses-pretty-gen "r")})))

(fact "clauses-init will automatically relate refs"
  (clauses-init c2-data)
  => '[[?e :category/name "root"]
       [?e :category/image ?r2]
       [?e :category/image ?r1]
       [?r2 :image/type :small]
       [?r1 :image/type :big]]
  (clauses c1-data c1-geni))

(def c3-data
  (->
   {:#/sym '?e
    :#/not {:category/name "chris"
            :category/tags #{"happy" "sad"}}}
   (process c1-geni {:sets-only? true})
   (characterise (flatten-all-keys c1-geni) {:generate-syms true})))

(fact
  (clauses-not c3-data c1-opts)
  =>  '[[?e :category/name ?ng1]
        [(not= ?ng1 "chris")]
        [?e :category/tags ?ng2]
        [(not= ?ng2 "happy")]
        [?e :category/tags ?ng3]
        [(not= ?ng3 "sad")]])

(def c4-data
  (->
   {:#/sym '?e
    :#/fulltext {:category/name "chris"
                 :category/tags #{"happy" "sad"}}}
   (process c1-geni {:sets-only? true})
   (characterise (flatten-all-keys c1-geni) {:generate-syms true})))

(fact "fulltext clauses"
  (clauses-fulltext c4-data c1-opts)
  => '[[(fulltext $ :category/name "chris") [[?e ?ft1]]]
       [(fulltext $ :category/tags "happy") [[?e ?ft2]]]
       [(fulltext $ :category/tags "sad") [[?e ?ft3]]]])


(def c5-data
  (-> {:#/sym      '?e
       :category   {:name "root"
                    :tags #{"shop" "new"}}
       :#/not      {:category/name "chris"
                    :category/tags #{"happy" "sad"}}
       :#/fulltext {:category/name "chris"
                    :category/tags #{"happy" "sad"}}
       :#/q        '[[?e :category/name "hello"]]}
      (process c1-geni {:sets-only? true})
      (characterise (flatten-all-keys c1-geni) {:generate-syms true})))

(fact "a bunch of mixed clauses"
  (clauses c5-data c1-opts)
  => '[:find ?e :where
       [?e :category/tags "new"]
       [?e :category/tags "shop"]
       [?e :category/name "root"]
       [?e :category/name ?ng1]
       [(not= ?ng1 "chris")]
       [?e :category/tags ?ng2]
       [(not= ?ng2 "happy")]
       [?e :category/tags ?ng3]
       [(not= ?ng3 "sad")]
       [(fulltext $ :category/name "chris") [[?e ?ft1]]]
       [(fulltext $ :category/tags "happy") [[?e ?ft2]]]
       [(fulltext $ :category/tags "sad") [[?e ?ft3]]]
       [?e :category/name "hello"]])
