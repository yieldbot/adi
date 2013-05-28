(ns adi.test-api-linked
 (:use midje.sweet
       adi.schema
       hara.common
       hara.checkers
       [adi.utils :only [iid]]
       [adi.emit.datoms :only [emit-datoms-insert]]
       [adi.emit.query :only [emit-query]]
       [adi.emit.process :only [process-init-env]])
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

(def l1-data
  {:db/id (iid :start)
   :link {:value "l1"
          :next {:value "l2"
                 :next {:value "l3"
                        :next {:+ {:db/id (iid :start)}}}}}})


(emit-datoms-insert l1-data l1-env)
(aa/install-schema *conn* l1-fgeni)
(aa/insert! *conn* l1-data l1-env)


(fact "select"
  (emit-query {:link/value "l1"} l1-env)
  => (just-in [:find symbol? :where
               [symbol? :link/value "l1"]])

  (-> (aa/select-entities
       (d/db *conn*)
       '[:find ?x :where
         [?x :link/value "l1"]]
         {})
      first
      seq)
  => (contains [[:link/value "l1"]])

  (-> (aa/select-entities (d/db *conn*) {:link/value "l1"} l1-env)
      first
      seq)
  => (contains [[:link/value "l1"]])

  (-> (aa/select-entities (d/db *conn*) {:link/prev/value "l3"} l1-env)
      first
      seq)
  => (contains [[:link/value "l1"]])

  (aa/select (d/db *conn*) {:link/value "l1"}
             (assoc l1-env :view {:link {:value :show :next :show}}))
  => (contains-in [{:link {:next anything, :value "l1"}}])

  (aa/select (d/db *conn*) {:link/next/value "l2"}
             (assoc l1-env :view {:link {:value :show :next :show}}))
  => (contains-in [{:link {:next anything, :value "l1"}}])

  (aa/select (d/db *conn*) {:link/prev/value "l3"}
             (assoc l1-env :view {:link {:value :show :next :show}}))
  => (contains-in [{:link {:next anything, :value "l1"}}]))


(fact "select"
  (aa/select (d/db *conn*)
             {:link/prev/value "l3"}
             (assoc l1-env :view {:link/value :show}))
  => (just-in [{:link {:value "l1"}, :db anything}])


  (aa/select (d/db *conn*)
             {:link/next/next/value "l3"}
             (assoc l1-env :view {:link/value :show}))
  => (just-in [{:link {:value "l1"}, :db anything}])

  (aa/select (d/db *conn*)
             {:link/value "l1"}
             (assoc l1-env :view {:link/next :follow :link/value :show}))
  => (just-in [{:db anything
                :link {:value "l1"
                       :next {:+ anything
                              :value "l2"
                              :next {:+ anything
                                     :value "l3"
                                     :next anything}}}}]))


(fact "reverse selection"
  (aa/select (d/db *conn*)
             {:link/value "l1"}
             (assoc l1-env :view {:link/prev :show
                                  :link/value :show
                                  :link/next :follow}))
  => (contains-in [{:link {:value "l1"
                           :prev #{hash-map?}
                           :next {:value "l2"
                                  :prev #{hash-map?}
                                  :next {:value "l3"
                                         :prev #{hash-map?}
                                         :next hash-map?}}}}])

  (aa/select (d/db *conn*)
             {:link/value "l1"}
             (assoc l1-env :view {:link/prev :follow
                                  :link/value :show}))
  => (contains-in [{:link {:value "l1"
                           :prev #{{:value "l3"
                                    :prev #{{:prev #{hash-map?},
                                             :value "l2"}}}}}}])

  (aa/select (d/db *conn*)
             {:link/value "l3"}
             (assoc l1-env :view {:link/prev :follow
                                  :link/value :show}))

  => (contains-in [{:link {:value "l3"
                           :prev #{{:value "l2"
                                    :prev #{{:prev #{hash-map?},
                                             :value "l1"}}}}}}]))
