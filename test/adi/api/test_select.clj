(ns adi.api.test-select
 (:use midje.sweet
       adi.utils
       adi.schema
       adi.data
       adi.checkers)
 (:require [datomic.api :as d]
           [adi.api :as aa]))

(def ^:dynamic *uri* "datomic:mem://adi-test-select")
(def ^:dynamic *conn* (aa/connect! *uri* true))


(def s0-geni
  (add-idents {:account {:cars  [{:type    :long}]
                         :name  [{:type    :string}]}}))

(def s0-opts {:geni s0-geni
              :fgeni (flatten-all-keys s0-geni)})

(fact "Loading up simple data"
  (against-background
    [(before :contents
             (do
              (def ^:dynamic *conn* (aa/connect! *uri* true))
              (aa/install-schema s0-geni *conn*)
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
                          *conn* s0-opts)))])

  (aa/select {:account/name "chris"} (d/db *conn*) s0-opts)
  => (two-of (contains {:account (contains {:name "chris"})}))

  (aa/select {:account/cars 1} (d/db *conn*) s0-opts)
  => (four-of (contains {:account (contains {:cars 1})}))

  (aa/select {:account {:cars 1
                        :name "dave"}} (d/db *conn*) s0-opts)
  => (two-of truthy)

  (aa/select {:account {:cars 1
                        :name "dave"
                        :extra "something"}} (d/db *conn*) s0-opts)
  => (throws Exception)

  (aa/select {:account {:cars 1
                        :name "dave"
                        :extra "something"}} (d/db *conn*)
                        (assoc s0-opts :extras? true))
  => (two-of truthy))


(def s1-geni
  (add-idents
   {:category {:name         [{:type        :string
                               :fulltext    true}]
               :tags         [{:type        :string
                               :cardinality :many}]
               :image        [{:type        :ref
                               :ref-ns      :image}]
               :children     [{:type        :ref
                               :ref-ns      :category
                               :cardinality :many}]}
    :image     {:url         [{:type        :string}]
                :type        [{:type        :keyword}]}}))

(def s1-opts {:geni s1-geni
             :fgeni (flatten-all-keys s1-geni)})


(fact "Loading up simple data"
  (against-background
    [(before :contents
             (do
              (def ^:dynamic *conn* (aa/connect! *uri* true))
              (aa/install-schema s1-geni *conn*)
              (aa/insert! [{:category/name "root"}
                           {:category/name "l 1"}
                           {:category/name "l 2"}
                           {:category/name "l 3"}
                           {:category/name "l 4"}
                           {:category/name "l 5"}
                           {:category/name "l 6"}]
                          *conn* s1-opts)))])

  (aa/select :category/name (d/db *conn*) s1-opts)
  => (exclude-ids
      [{:category {:name "root"}}
       {:category {:name "l 1"}}
       {:category {:name "l 2"}}
       {:category {:name "l 3"}}
       {:category {:name "l 4"}}
       {:category {:name "l 5"}}
       {:category {:name "l 6"}}])

  (aa/select {:category/name "root"} (d/db *conn*) s1-opts)
  => (exclude-ids
      [{:category {:name "root"}}])

  (aa/select {:#/fulltext {:category/name "l"}} (d/db *conn*) s1-opts)
  => (exclude-ids
      [{:category {:name "l 1"}}
       {:category {:name "l 2"}}
       {:category {:name "l 3"}}
       {:category {:name "l 4"}}
       {:category {:name "l 5"}}
       {:category {:name "l 6"}}])

  (do (aa/update! {:#/fulltext {:category/name "l"}}
                  {:category/tags #{"love" "happy"}}
                  *conn*
                  s1-opts)
      (aa/select {:#/fulltext {:category/name "l"}} (d/db *conn*) s1-opts))
  => (exclude-ids
      [{:category {:name "l 1" :tags #{"love" "happy"}}}
       {:category {:name "l 2" :tags #{"love" "happy"}}}
       {:category {:name "l 3" :tags #{"love" "happy"}}}
       {:category {:name "l 4" :tags #{"love" "happy"}}}
       {:category {:name "l 5" :tags #{"love" "happy"}}}
       {:category {:name "l 6" :tags #{"love" "happy"}}}])

  (do (aa/retract! {:#/fulltext {:category/name "l"}}
                   [:category/tags]
                   *conn*
                   s1-opts)
      (aa/select {:#/fulltext {:category/name "l"}} (d/db *conn*) s1-opts))
  => (exclude-ids
      [{:category {:name "l 1"}}
       {:category {:name "l 2"}}
       {:category {:name "l 3"}}
       {:category {:name "l 4"}}
       {:category {:name "l 5"}}
       {:category {:name "l 6"}}])

  (do (aa/delete! {:#/fulltext {:category/name "l"}}
                  *conn*
                  s1-opts)
      (aa/select :category/name (d/db *conn*) s1-opts))
  => (exclude-ids
      [{:category {:name "root"}}]))
