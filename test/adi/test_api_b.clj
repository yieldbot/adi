(ns adi.test-api-b
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
(aa/install-schema *conn* l1-fgeni)


(def l1-data
  {:db/id (iid :start)
   :link {:value "l1"
          :next {:value "l2"
                 :next {:value "l3"
                        :next {:+ {:db/id (iid :start)}}}}}})
(aa/insert! *conn* l1-data l1-env)

(def l1a-ent (aa/select-first-entity (d/db *conn*) {:node/value "l1A"}  l1-env))
