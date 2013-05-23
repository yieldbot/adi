(ns adi.test-api
 (:use midje.sweet
       adi.schema
       adi.utils
       hara.common
       hara.checkers
       adi.emit.query
       [adi.emit.view :only [view]]
       [adi.emit.process :only [process-init-env]])
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
    (aa/install-schema *conn* (-> s0-env :schema :fgeni))
    (aa/insert! *conn*
                [{:account {:cars 2 :name "adam"}}
                 {:account {:cars 1 :name "adam"}}
                 {:account {:cars 0 :name "bob"}}
                 {:account {:cars 1 :name "bob"}}
                 {:account {:cars 4 :name "bob"}}
                 {:account {:cars 0 :name "chris"}}
                 {:account {:cars 2 :name "chris"}}
                 {:account {:cars 1 :name "dave"}}
                 {:account {:cars 1 :name "dave"}}
                 {:account {:cars 2 :name "dave"}}]
                 s0-env)))

(fact "select"
  (aa/select (d/db *conn*) {:account/name "chris"} s0-env)
  => (just-in [{:account {:cars 0, :name "chris"}, :db hash-map?}
               {:account {:cars 2, :name "chris"}, :db hash-map?}]
              :in-any-order)

  (aa/select (d/db *conn*) {:account {:cars (?q < 2)}} s0-env)
  => (just-in [{:account {:name "adam", :cars 1}, :db hash-map?}
               {:account {:name "bob", :cars 0}, :db hash-map?}
               {:account {:name "bob", :cars 1}, :db hash-map?}
               {:account {:name "chris", :cars 0}, :db hash-map?}
               {:account {:name "dave", :cars 1}, :db hash-map?}
               {:account {:name "dave", :cars 1}, :db hash-map?}]
              :in-any-order)

  (aa/select (d/db *conn*)
             {:account {:cars (?q < 2)
                        :name #{(?not "bob") (?not "dave")}}} 
              s0-env)
  => (just-in [{:account {:name "adam", :cars 1}, :db hash-map?}
               {:account {:name "chris", :cars 0}, :db hash-map?}])

  (aa/select (d/db *conn*)
             {:account {:cars (?q < 2)
                        :name #{(?not "bob") (?not "dave")}}}
             (assoc s0-env :view {:account/name :hide}))
  => (just-in [{:account {:cars 1}, :db hash-map?}
               {:account {:cars 0}, :db hash-map?}]))

(fact "update"
  (aa/update- (d/db *conn*)
              {:account/name "chris"}
              {:account/cars 1}
              s0-env)
  => (just-in [{:db/id long?, :account/cars 1}
               {:db/id long?, :account/cars 1}]))

(fact "retract"
  (aa/retract- (d/db *conn*)
               {:account/name "chris"}
               [:account/cars]
               s0-env)
  => (just-in [[:db/retract long? :account/cars 0]
            [:db/retract long? :account/cars 2]]))


(fact "delete"
  (aa/delete- (d/db *conn*)
              {:account/name "chris"}
              s0-env)
  (just-in [[:db.fn/retractEntity long?]
         [:db.fn/retractEntity long?]]))


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

(def l1-env
  (process-init-env {:link {:value  [{:fulltext true}]
                            :next [{:type :ref
                                    :ref  {:ns :link
                                           :rval :prev}}]
                            :node [{:type :ref
                                    :ref {:ns :node}}]}
                     :node {:value  [{}]
                            :parent [{:type :ref
                                        :ref  {:ns :node
                                               :rval :children}}]}}))
(def l1-fgeni (-> l1-env :schema :fgeni))
(aa/install-schema *conn* l1-fgeni)


(def l1-data
  {:db/id (iid :start)
   :link {:value "l1"
          :next {:value "l2"
                 :next {:value "l3"
                        :next {:+ {:db/id (iid :start)}}}}}})
(aa/insert! *conn* l1-data l1-env)

(fact "select"
  (d/q '[:find ?v :where [?e :node/value ?v]] (d/db *conn*))
  => #{["root"]
       ["l1A"] ["l1A l2A"] ["l1A l2B"] ["l1A l2C"] ["l1A l2D"]
       ["l1B"] ["l1B l2A"] ["l1B l2B"] ["l1B l2C"] ["l1B l2D"]
       ["l1C"] ["l1C l2A"] ["l1C l2B"] ["l1C l2C"] ["l1C l2D"]}

  (aa/select (d/db *conn*) {:node/value (?fulltext "l2A")} d1-env)
  => (just-in [{:node {:value "l1A l2A", :parent anything} :db anything}
               {:node {:value "l1B l2A", :parent anything} :db anything}
               {:node {:value "l1C l2A", :parent anything} :db anything}]
              :in-any-order)

  (aa/select (d/db *conn*){:node/value (?fulltext "l2A")}
             (assoc d1-env :view {:node/parent :hide}))
  => (just-in [{:node {:value "l1A l2A"} :db anything}
               {:node {:value "l1B l2A"} :db anything}
               {:node {:value "l1C l2A"} :db anything}]
              :in-any-order)

  (aa/select (d/db *conn*) {:node/value "l1A"}
             (assoc d1-env :view {:node/parent :hide
                                  :node/children :show}))
  => (just-in
      [{:db anything
        :node {:value "l1A"
               :children [{:value "l1A l2A" :+ anything}
                          {:value "l1A l2B" :+ anything}
                          {:value "l1A l2C" :+ anything}
                          {:value "l1A l2D" :+ anything}]}}])


  (aa/select (d/db *conn*) {:node/value (?fulltext "l1C")} d1-env)
  => (just-in [{:node {:value "l1C", :parent anything}, :db anything}
               {:node {:value "l1C l2A", :parent anything} :db anything}
               {:node {:value "l1C l2B", :parent anything} :db anything}
               {:node {:value "l1C l2C", :parent anything} :db anything}
               {:node {:value "l1C l2D", :parent anything} :db anything}]
           :in-any-order)

  (aa/select (d/db *conn*) {:node {:children {:value "l1C"}}} d1-env)
  => (just-in [{:node {:value "root"}, :db anything}])

  (aa/select (d/db *conn*) {:node {:parent {:value "l1C"}}} d1-env)
  => (just-in [{:node {:value "l1C l2A", :parent anything} :db anything}
               {:node {:value "l1C l2B", :parent anything} :db anything}
               {:node {:value "l1C l2C", :parent anything} :db anything}
               {:node {:value "l1C l2D", :parent anything} :db anything}]
           :in-any-order))

(d1-fgeni :node/parent)

(def l1a-ent (aa/select-first-entity (d/db *conn*) {:node/value "l1A"}  d1-env))

(fact
  (aa/linked-nss d1-fgeni {:node/parent :ids})
  => #{}

  (aa/linked-nss d1-fgeni {:node/parent :show})
  => #{:node/parent}

  (aa/linked-nss d1-fgeni {:node/children :show
                           :node/parent :ids})
  => #{:node/children}

  (aa/linked-ids-key :node/parent l1a-ent
                         #{:node/parent} d1-env #{})
  => (one-of long?)

  (aa/linked-ids l1a-ent {:node/parent :show} d1-env)
  => (two-of long?)


  (aa/linked-ids-key :node/children l1a-ent
                         #{:node/children} d1-env #{})
  => (four-of long?)

  (aa/linked-ids l1a-ent {:node/children :show} d1-env)
  => (five-of long?)

  (aa/linked-ids l1a-ent {:node/parent :show
                              :node/children :show} d1-env)
  => (n-of long? 16)

  (aa/linked-ids
   (aa/select-first-entity (d/db *conn*) {:node/value "l1A"}
                           d1-env)
   {:node/children :show}
   d1-env))

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
			:in-any-order))
