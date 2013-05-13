(ns adi.emit.test-query
  (use hara.common
       adi.emit.query
       adi.utils
       adi.data.samples
       [adi.emit.characterise :only [characterise]]
       midje.sweet))

(fact "?q macro"
  (?q < 3) => '[[??sym ??attr ??] [(< ?? 3)]]
  (?q not= 3) => '[[??sym ??attr ??] [(not= ?? 3)]])

(fact "?not"
  (?not 9) => '[[??sym ??attr ??] [(not= ?? 9)]]
  (?not "this") => '[[??sym ??attr ??] [(not= ?? "this")]])

(fact "?fulltext"
  (?fulltext "hello") => '[[(fulltext $ ??attr "hello") [[??sym ??]]]])

(defn query-env [env]
  (merge-nested env
            {:options {:sets-only? true
                       :query? true}
             :generate {:syms {:function (incremental-sym-gen 'e)
                        :not {:function (incremental-sym-gen 'n)}
                        :fulltext {:function (incremental-sym-gen 'ft)}}}}))

(facts "query-init"
  (query-data {:data-many {:account/name #{"chris"}}
               :# {:sym '?e1}}
              (query-env s7-env))
  => '[[?e1 :account/name "chris"]]

  (query-data {:data-many {:account/name #{(?q = "hello")}}
                         :# {:sym '?x}}
                        (query-env s7-env))
  => '[[?x :account/name ?e1]
       [(= ?e1 "hello")]]

  (query-data {:data-many {:account/name #{"adam" "bob" "chris"}}
               :# {:sym '?e1}}
              s7-env)
  => (just '[[?e1 :account/name "adam"]
             [?e1 :account/name "bob"]
             [?e1 :account/name "chris"]] :in-any-order)

  (query-refs (characterise {:node/children #{{}}
                               :node/parent #{{}}}
                              (query-env s6-env)))
  => '[[?e1 :node/parent ?e3]
       [?e2 :node/parent ?e1]]

  (query-init (characterise {:account/name #{"adam" "chris"}} (query-env s7-env)) s7-env)
  => '[[?e1 :account/name "adam"]
       [?e1 :account/name "chris"]]

  (query-init (characterise {:node/value #{"undefined"}
                               :node/children #{{:node/value #{"child1"}}}
                               :node/parent #{{:node/value #{"parent1"}}}}
                            (query-env s6-env))
              s6-env)
  => '[[?e1 :node/value "undefined"]
       [?e3 :node/parent ?e1]
       [?e1 :node/parent ?e2]
       [?e3 :node/value "child1"]
       [?e2 :node/value "parent1"]])

(facts "other query"
  (query-q {:# {:q '[?e :node/value "root"]}})
  => '[?e :node/value "root"]

  (query-data-val '?x :node/value '[[??sym ??attr ??]
                                    [(not= ?? "undefined")]]
                  (query-env s6-env))
  => '[[?x :node/value ?e1]
       [(not= ?e1 "undefined")]]

  (query-data (characterise {:node/value #{(?q < 4)}} (query-env s6-env))
              (query-env s6-env))
  => '[[?e1 :node/value ?e1]
       [(< ?e1 4)]]

  (query (characterise {:# {:sym '?x}
                        :node/parent #{{:node/value #{"root"}}}
                        :node/value #{(?fulltext "sub")}} (query-env s6-env))
         (query-env s6-env))
  => '[:find ?x :where
       [(fulltext $ :node/value "sub") [[?x ?e1]]]
       [?x :node/parent ?e2]
       [?e2 :node/value "root"]])

(fact "emit-query"
  (emit-query {:account {:name "chris"}} s7-env)
  => (just [:find anything :where
            (just [anything :account/name "chris"])])

  (emit-query {:account {:name "chris"}} (query-env s7-env))
  => '[:find ?e1 :where
       [?e1 :account/name "chris"]]

  (emit-query {:node/parent {:value "root"}} (query-env s6-env))
  => '[:find ?e1 :where
       [?e1 :node/parent ?e2]
       [?e2 :node/value "root"]]

  (emit-query {:node/children {:value "root"}} (query-env s6-env))
  => '[:find ?e1 :where
       [?e2 :node/parent ?e1]
       [?e2 :node/value "root"]]

  (emit-query {:account/id #{(?q > 3) (?q < 6)}} (query-env s7-env))
  => '[:find ?e1 :where
       [?e1 :account/id ?e2]
       [(> ?e2 3)]
       [?e1 :account/id ?e3]
       [(< ?e3 6)]]

  (emit-query {:# {:sym '?x}
               :account/id #{(?q > 3) (?not 6)}
               :account/name (?fulltext "chris")}
              (query-env s7-env))
  => '[:find ?x :where
       [(fulltext $ :account/name "chris") [[?x ?e2]]]
       [?x :account/id ?e3]
       [(not= ?e3 6)]
       [?x :account/id ?e4]
       [(> ?e4 3)]]

  (emit-query {:# {:sym '?x}
               :node/children/parent/parent/value (?not 4)}
              (query-env s6-env))
  => '[:find ?x :where
       [?e2 :node/parent ?x]
       [?e2 :node/parent ?e3]
       [?e3 :node/parent ?e4]
       [?e4 :node/value ?e5]
       [(not= ?e5 4)]])
