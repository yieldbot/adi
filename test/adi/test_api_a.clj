(ns adi.test-api-a
 (:use midje.sweet
       adi.schema
       adi.utils
       hara.common
       hara.checkers
       adi.emit.query
       [adi.emit.view :only [view]]
       [adi.emit.process :only [process-init-env]])
 (:require [datomic.api :as d]
           [adi.api :as aa]
           [adi.emit.reap :as ar]))

(def d1-env
  (process-init-env {:node {:value  [{:default "undefined"
                                      :fulltext true}]
                            :parent [{:type :ref
                                      :ref  {:ns :node
                                             :rval :children}}]}
                     :leaf {:value [{:default "leafy"}]
                          :node  [{:type :ref
                                   :ref {:ns :node
                                         :rval :leaves}}]}}))

(def d1-fgeni (-> d1-env :schema :fgeni))

(def ^:dynamic *uri* "datomic:mem://adi-test-api-linked")
(def ^:dynamic *conn* (aa/connect! *uri* true))
(aa/install-schema *conn* d1-fgeni)
(aa/insert! *conn*
            {:node {:value "root"
                    :children #{{:value "l1A"
                                 :children #{{:value "l1A l2A"}
                                             {:value "l1A l2B"}
                                             {:value "l1A l2C"}
                                             {:value "l1A l2D"}}}
                                {:value "l1B"
                                 :children #{{:value "l1B l2A"}
                                             {:value "l1B l2B"}
                                             {:value "l1B l2C"}
                                             {:value "l1B l2D"}}}
                                {:value "l1C"
                                 :children #{{:value "l1C l2A"}
                                             {:value "l1C l2B"}
                                             {:value "l1C l2C"}
                                             {:value "l1C l2D"}}}}}}
            d1-env)

(fact "select"
  (d/q '[:find ?v :where [?e :node/value ?v]] (d/db *conn*))
  => #{["root"]
       ["l1A"] ["l1A l2A"] ["l1A l2B"] ["l1A l2C"] ["l1A l2D"]
       ["l1B"] ["l1B l2A"] ["l1B l2B"] ["l1B l2C"] ["l1B l2D"]
       ["l1C"] ["l1C l2A"] ["l1C l2B"] ["l1C l2C"] ["l1C l2D"]}

  (aa/select (d/db *conn*) {:node/value (?fulltext "l2A")}
             (assoc d1-env :view :node))
  => (just-in [{:node {:value "l1A l2A", :parent anything} :db anything}
               {:node {:value "l1B l2A", :parent anything} :db anything}
               {:node {:value "l1C l2A", :parent anything} :db anything}]
              :in-any-order)

  (aa/select (d/db *conn*){:node/value (?fulltext "l2A")}
             (assoc d1-env :view {:node {:parent :hide
                                         :value :show}}))
  => (just-in [{:node {:value "l1A l2A"} :db anything}
               {:node {:value "l1B l2A"} :db anything}
               {:node {:value "l1C l2A"} :db anything}]
              :in-any-order)

  (aa/select (d/db *conn*) {:node/value (?fulltext "l1C")}
             (assoc d1-env :view :node))
  => (just-in [{:node {:value "l1C", :parent anything}, :db anything}
               {:node {:value "l1C l2A", :parent anything} :db anything}
               {:node {:value "l1C l2B", :parent anything} :db anything}
               {:node {:value "l1C l2C", :parent anything} :db anything}
               {:node {:value "l1C l2D", :parent anything} :db anything}]
              :in-any-order)

  (aa/select (d/db *conn*) {:node {:children {:value "l1C"}}}
             (assoc d1-env :view :node))
  => (just-in [{:node {:value "root"}, :db anything}])

  (aa/select (d/db *conn*) {:node {:parent {:value "l1C"}}}
             (assoc d1-env :view :node))
  => (just-in [{:node {:value "l1C l2A", :parent anything} :db anything}
               {:node {:value "l1C l2B", :parent anything} :db anything}
               {:node {:value "l1C l2C", :parent anything} :db anything}
               {:node {:value "l1C l2D", :parent anything} :db anything}]
              :in-any-order))



(fact "select reverse test"
  (def ^:dynamic *res* (aa/select-entities (d/db *conn*) {:node/value "l1A"}
                                 d1-env))

  (ar/reap (first *res*)
           (assoc d1-env
             :view {:node/parent :hide
                    :node/value  :show
                    :node/children :follow}
             :reap {:ids :hide}))
  {:node {:value "l1A", :children #{{:value "l1A l2C"} {:value "l1A l2B"} {:value "l1A l2D"} {:value "l1A l2A"}}}}


  (aa/select (d/db *conn*) {:node/value "l1A"}
             (assoc d1-env
               :view {:node/parent :hide
                      :node/value  :show
                      :node/children :follow}
               :reap {:ids :hide}))

  => (just-in
                [{:node {:value "l1A"
                         :children [{:value "l1A l2A"}
                                    {:value "l1A l2B"}
                                    {:value "l1A l2C"}
                                    {:value "l1A l2D"}]}}]))

(fact
  (aa/linked-nss d1-fgeni {:node/parent :ids})
  => #{}

  (aa/linked-nss d1-fgeni {:node/parent :show})
  => #{:node/parent}

  (aa/linked-nss d1-fgeni {:node/children :show
                           :node/parent :ids})
  => #{:node/children}

  (aa/linked-ids
   (aa/select-first-entity (d/db *conn*) {:node/value "l1A"}
                           d1-env)
   {:node/children :show}
   d1-env))

(comment
  (fact "linked-entities"
    (view d1-fgeni)
    => {:node/value :show
        :node/children :hide
        :node/leaves :hide
        :node/parent :ids
        :leaf/node :ids
        :leaf/value :show}

    (aa/linked-entities (d/db *conn*) {:node/value "l1A"} d1-env)
    => (two-of ref?)

    (aa/linked-entities (d/db *conn*) {:node/value "l1A"}
                        (assoc d1-env :view {:node/children :show}))
    => (five-of ref?)

    (aa/linked (d/db *conn*) {:node/value "l1A"} d1-env)
    => (contains-in [{:node {:value "root"}}
                     {:node {:value "l1A"}}]
                    :in-any-order)

    (aa/linked (d/db *conn*) {:node/value "l1A"}
               (assoc d1-env :view {:node/children :show}))
    => (contains-in [{:node {:value "l1A l2B"}}
                     {:node {:value "l1A l2C"}}
                     {:node {:value "l1A l2D"}}
                     {:node {:value "l1A l2A"}}
                     {:node {:value "l1A"}}]
                    :in-any-order)

    (aa/linked (d/db *conn*) {:node/value "l1A"}
               (assoc d1-env :view {:node/children :show :node/parent :show}))
    => (contains-in [{:node {:value "root"}}
                     {:node {:value "l1C l2D"}}
                     {:node {:value "l1C l2A"}}
                     {:node {:value "l1C l2B"}}
                     {:node {:value "l1C l2C"}}
                     {:node {:value "l1C"}}
                     {:node {:value "l1A l2B"}}
                     {:node {:value "l1A l2C"}}
                     {:node {:value "l1A l2D"}}
                     {:node {:value "l1A l2A"}}
                     {:node {:value "l1A"}}
                     {:node {:value "l1B l2C"}}
                     {:node {:value "l1B l2A"}}
                     {:node {:value "l1B l2D"}}
                     {:node {:value "l1B l2B"}}
                     {:node {:value "l1B"}}]
                    :in-any-order)
    (aa/linked (d/db *conn*) {:node/value "l1A l2A"}
               (assoc d1-env :view {:node/parent :show}))
    => (just [(contains-in {:node {:value "root"}})
              (contains-in {:node {:value "l1A"}})
              (contains-in {:node {:value "l1A l2A"}})]
             :in-any-order)))
