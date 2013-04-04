(ns adi.test-api
 (:use midje.sweet
       adi.utils
       adi.schema
       adi.data
       adi.checkers)
 (:require [datomic.api :as d]
           [adi.api :as aa]))

(def ^:dynamic *uri* "datomic:mem://adi-test-api")
(def ^:dynamic *conn* (aa/connect! *uri* true))


(def s0-sgeni
  {:account {:cars  [{:type    :long}]
             :name  [{:type    :string}]}})

(def s0-env (process-init-env s0-sgeni {}))

(fact
  (do
    (def ^:dynamic *conn* (aa/connect! *uri* true))
    (aa/install-schema (-> s0-env :schema :fgeni) *conn*)
    (aa/insert! [{:account {:cars 2 :name "adam"}}
                 {:account {:cars 1 :name "adam"}}
                 {:account {:cars 0 :name "bob"}}
                 {:account {:cars 1 :name "bob"}}
                 {:account {:cars 4 :name "bob"}}
                 {:account {:cars 0 :name "chris"}}
                 {:account {:cars 2 :name "chris"}}
                 {:account {:cars 1 :name "dave"}}
                 {:account {:cars 1 :name "dave"}}
                 {:account {:cars 2 :name "dave"}}]
                *conn* s0-env)))

(fact "select"
  (aa/select {:account/name "chris"} (d/db *conn*) s0-env)
  => (just [(just {:account {:cars 0, :name "chris"}, :db hash-map?})
            (just {:account {:cars 2, :name "chris"}, :db hash-map?})]
           :in-any-order)

  (aa/select {:account {:cars (? < 2)}} (d/db *conn*) s0-env)
  => (just [(just {:account {:name "adam", :cars 1}, :db hash-map?})
            (just {:account {:name "bob", :cars 0}, :db hash-map?})
            (just {:account {:name "bob", :cars 1}, :db hash-map?})
            (just {:account {:name "chris", :cars 0}, :db hash-map?})
            (just {:account {:name "dave", :cars 1}, :db hash-map?})
            (just {:account {:name "dave", :cars 1}, :db hash-map?})]
           :in-any-order)

  (aa/select {:account {:cars (? < 2)
                        :name #{(?not "bob") (?not "dave")}}} (d/db *conn*) s0-env)
  => (just [(just {:account {:name "adam", :cars 1}, :db hash-map?})
            (just {:account {:name "chris", :cars 0}, :db hash-map?})])

  (aa/select {:account {:cars (? < 2)
                        :name #{(?not "bob") (?not "dave")}}}
             (d/db *conn*)
             (assoc s0-env :view {:account/name :hide}))
  => (just [(just {:account {:cars 1}, :db hash-map?})
            (just {:account {:cars 0}, :db hash-map?})]))

(fact "update"
  (aa/update- {:account/name "chris"}
              {:account/cars 1}
              (d/db *conn*)
              s0-env)
  => (just [(just {:db/id long?, :account/cars 1})
            (just {:db/id long?, :account/cars 1})]))

(fact "retract"
  (aa/retract- {:account/name "chris"}
               [:account/cars]
               (d/db *conn*)
               s0-env)
  => (just [(just [:db/retract long? :account/cars 0])
            (just [:db/retract long? :account/cars 2])]))


(fact "delete"
  (aa/delete- {:account/name "chris"}
              (d/db *conn*)
              s0-env)
  (just [(just [:db.fn/retractEntity long?])
         (just [:db.fn/retractEntity long?])]))


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

(aa/install-schema d1-fgeni *conn*)
(aa/insert! {:node {:value "root"
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
            *conn* d1-env)



(fact "select"
  (d/q '[:find ?v :where [?e :node/value ?v]] (d/db *conn*))
  => #{["root"]
       ["l1A"] ["l1A l2A"] ["l1A l2B"] ["l1A l2C"] ["l1A l2D"]
       ["l1B"] ["l1B l2A"] ["l1B l2B"] ["l1B l2C"] ["l1B l2D"]
       ["l1C"] ["l1C l2A"] ["l1C l2B"] ["l1C l2C"] ["l1C l2D"]}

  (aa/select {:node/value (?fulltext "l2A")} (d/db *conn*) d1-env)
  => (just-in [{:node {:value "l1A l2A", :parent anything} :db anything}
               {:node {:value "l1B l2A", :parent anything} :db anything}
               {:node {:value "l1C l2A", :parent anything} :db anything}]
              :in-any-order)

  (aa/select {:node/value (?fulltext "l2A")} (d/db *conn*)
             (assoc d1-env :view {:node/parent :hide}))
  => (just-in [{:node {:value "l1A l2A"} :db anything}
               {:node {:value "l1B l2A"} :db anything}
               {:node {:value "l1C l2A"} :db anything}]
              :in-any-order)

  (aa/select {:node/value "l1A"} (d/db *conn*)
             (assoc d1-env :view {:node/parent :hide
                                  :node/children :show}))
  => (just-in
      [{:db anything
        :node {:value "l1A"
               :children [{:value "l1A l2A" :+ anything}
                          {:value "l1A l2B" :+ anything}
                          {:value "l1A l2C" :+ anything}
                          {:value "l1A l2D" :+ anything}]}}])


  (aa/select {:node/value (?fulltext "l1C")} (d/db *conn*) d1-env)
  => (just-in [{:node {:value "l1C", :parent anything}, :db anything}
               {:node {:value "l1C l2A", :parent anything} :db anything}
               {:node {:value "l1C l2B", :parent anything} :db anything}
               {:node {:value "l1C l2C", :parent anything} :db anything}
               {:node {:value "l1C l2D", :parent anything} :db anything}]
           :in-any-order)

  (aa/select {:node {:children {:value "l1C"}}} (d/db *conn*) d1-env)
  => (just-in [{:node {:value "root"}, :db anything}])

  (aa/select {:node {:parent {:value "l1C"}}} (d/db *conn*) d1-env)
  => (just-in [{:node {:value "l1C l2A", :parent anything} :db anything}
               {:node {:value "l1C l2B", :parent anything} :db anything}
               {:node {:value "l1C l2C", :parent anything} :db anything}
               {:node {:value "l1C l2D", :parent anything} :db anything}]
           :in-any-order))
