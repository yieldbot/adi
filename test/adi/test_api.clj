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

#_(fact "select"
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
            (just {:account {:name "chris", :cars 0}, :db hash-map?})]))

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


(fact "emit-path"
  {:node/parent :id
   :node/children :id
   :node/value :show})

(def s6-sgeni {:node {:value  [{:default "undefined"
                                :fulltext true}]
                      :parent [{:type :ref
                                :ref  {:ns :node
                                       :rval :children}}]}})
(def s6-env (process-init-env s6-sgeni {}))

(def s6-view
  {:data {:node/value :show}
   :refs {:node/parent :show}})

(def s6-path
  {:node #{:node/parent}})

#_(fact "emit-view"
  (view (-> s6-env :schema :fgeni))
  => {:data {:node/value :show}
      :refs {:node/parent :ids}
      :revs {:node/children :hide}})

#_(fact "emit-path"
  (emit-path s6-view)
  => {:node #{:node/parent}})

(aa/install-schema (-> s6-env :schema :fgeni) *conn*)
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
            *conn* s6-env)

#_(fact "select"
  (aa/select {:node/value (?fulltext "l2A")} (d/db *conn*) s6-env)
  => (just [(just {:node (just {:value "l1A l2A", :parent anything}) :db anything})
            (just {:node (just {:value "l1B l2A", :parent anything}) :db anything})
            (just {:node (just {:value "l1C l2A", :parent anything}) :db anything})]
           :in-any-order)

  (aa/select {:node/value (?fulltext "l1C")} (d/db *conn*) s6-env)
  => (just [(just {:node (just {:value "l1C", :parent anything}), :db anything})
            (just {:node (just {:value "l1C l2A", :parent anything}) :db anything})
            (just {:node (just {:value "l1C l2B", :parent anything}), :db anything})
            (just {:node (just {:value "l1C l2C", :parent anything}), :db anything})
            (just {:node (just {:value "l1C l2D", :parent anything}), :db anything})]
           :in-any-order)

  (aa/select {:node {:children {:value "l1C"}}} (d/db *conn*) s6-env)
  => (just [(contains {:node {:value "root"}, :db anything})])

  (aa/select {:node {:parent {:value "l1C"}}} (d/db *conn*) s6-env)
  => (just [(just {:node (just {:value "l1C l2A", :parent anything}) :db anything})
            (just {:node (just {:value "l1C l2B", :parent anything}), :db anything})
            (just {:node (just {:value "l1C l2C", :parent anything}), :db anything})
            (just {:node (just {:value "l1C l2D", :parent anything}), :db anything})]
           :in-any-order))
