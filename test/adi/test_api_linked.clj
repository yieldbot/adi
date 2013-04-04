(ns adi.test-api-linked
 (:use midje.sweet
       adi.utils
       adi.schema
       adi.data
       adi.checkers)
 (:require [datomic.api :as d]
           [adi.api :as aa]))

(def ^:dynamic *uri* "datomic:mem://adi-test-api-linked")
(def ^:dynamic *conn* (aa/connect! *uri* true))

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

(aa/install-schema l1-fgeni *conn*)
(aa/insert! {:db/id (iid :start)
             :link {:value "l1"
                    :next {:value "l2"
                           :next {:value "l3"
                                  :next {:+ {:db/id (iid :start)}}}}}}
            *conn* l1-env)

(fact "select"
  (emit-query {:link/value "l1"} l1-env)
  => (just-in [:find symbol? :where
               [symbol? :link/value "l1"]])

  (-> (aa/select-entities
       '[:find ?x :where
         [?x :link/value "l1"]] (d/db *conn*)
         {})
      first
      seq)
  => (contains [[:link/value "l1"]])


  (-> (aa/select-entities {:link/value "l1"} (d/db *conn*) l1-env)
      first
      seq)
  => (contains [[:link/value "l1"]])

  (-> (aa/select-entities {:link/prev/value "l3"} (d/db *conn*) l1-env)
      first
      seq)
  => (contains [[:link/value "l1"]])

  (aa/select {:link/value "l1"} (d/db *conn*) l1-env)
  => (contains-in [{:link {:next anything, :value "l1"}}])

  (aa/select {:link/next/value "l2"} (d/db *conn*) l1-env)
  => (contains-in [{:link {:next anything, :value "l1"}}])

  (aa/select {:link/prev/value "l3"} (d/db *conn*) l1-env)
  => (contains-in [{:link {:next anything, :value "l1"}}]))


(fact "select"
  (aa/select {:link/prev/value "l3"}
             (d/db *conn*)
             (assoc l1-env :view {:link/next :hide}))
  => (just-in [{:link {:value "l1"}, :db anything}])


  (aa/select {:link/next/next/value "l3"}
             (d/db *conn*)
             (assoc l1-env :view {:link/next :hide}))
  => (just-in [{:link {:value "l1"}, :db anything}])

  (aa/select {:link/value "l1"}
             (d/db *conn*)
             (assoc l1-env :view {:link/next :show}))
  => (just-in [{:db anything
                :link {:value "l1"
                       :next {:+ anything
                              :value "l2"
                              :next {:+ anything
                                     :value "l3"
                                     :next anything}}}}]))


(fact "reverse selection"
  (aa/select {:link/value "l1"}
             (d/db *conn*)
             (assoc l1-env :view {:link/prev :ids
                                  :link/next :show}))
  => (contains-in [{:link {:value "l1"
                           :prev #{hash-map?}
                           :next {:value "l2"
                                  :prev #{hash-map?}
                                  :next {:value "l3"
                                         :prev #{hash-map?}
                                         :next hash-map?}}}}])

  (aa/select {:link/value "l1"}
             (d/db *conn*)
             (assoc l1-env :view {:link/prev :show
                                  :link/next :hide}))
  => (contains-in [{:link {:value "l1"
                           :prev #{{:value "l3"
                                    :prev #{{:prev #{hash-map?},
                                             :value "l2"}}}}}}])

  (aa/select {:link/value "l3"}
             (d/db *conn*)
             (assoc l1-env :view {:link/prev :show
                                  :link/next :hide}))

  => (contains-in [{:link {:value "l3"
                           :prev #{{:value "l2"
                                    :prev #{{:prev #{hash-map?},
                                             :value "l1"}}}}}}]))
