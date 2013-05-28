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
  (aa/select (d/db *conn*) {:account/name "chris"}
             (assoc s0-env :view :account))
  => (just-in [{:account {:cars 0, :name "chris"}, :db hash-map?}
               {:account {:cars 2, :name "chris"}, :db hash-map?}]
              :in-any-order)

  (aa/select (d/db *conn*) {:account {:cars (?q < 2)}}
             (assoc s0-env :view :account))
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
              (assoc s0-env :view :account))
  => (just-in [{:account {:name "adam", :cars 1}, :db hash-map?}
               {:account {:name "chris", :cars 0}, :db hash-map?}])

  (aa/select (d/db *conn*)
             {:account {:cars (?q < 2)
                        :name #{(?not "bob") (?not "dave")}}}
             (assoc s0-env :view {:account {:name :hide
                                            :cars :show}}))
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
