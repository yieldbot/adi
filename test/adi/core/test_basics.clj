(ns adi.core.test-basics
 (:use midje.sweet
       adi.utils
       adi.schema
       adi.core
       adi.checkers)
 (:require [datomic.api :as d]
           [adi.api :as aa]
           [adi.core :as adi]))

(def ^:dynamic *ds* nil)

(defn reset-database []
  (def ^:dynamic *ds*
    (datastore "datomic:mem://adi-core-test-basics"
               {:account {:id   [{:type :long}]
                          :name [{:type :string}]
                          :tags [{:type :string
                                  :cardinality :many}]}}
               true true))

  (adi/insert! [{:account {:id 0 :name "chris" :tags #{"g1" "sun"  "boo"}}}
                {:account {:id 1 :name "dave"  :tags #{"g1" "sun"  "boo"}}}
                {:account {:id 2 :name "chris" :tags #{"g2" "sun"  "boo"}}}
                {:account {:id 3 :name "dave"  :tags #{"g2" "sun"  "boo"}}}
                {:account {:id 4 :name "chris" :tags #{"g1" "moon" "boo"}}}
                {:account {:id 5 :name "dave"  :tags #{"g1" "moon" "boo"}}}
                {:account {:id 6 :name "chris" :tags #{"g2" "moon" "boo"}}}
                {:account {:id 7 :name "dave"  :tags #{"g2" "moon" "boo"}}}]
               *ds*))


(fact "select-ids"
  (against-background [(before :contents (reset-database))])

  (adi/select-ids {:account {:id '_}} *ds*)
  => (eight-of long?)

  (select-ids {:account/name "chris"} *ds*)
  => (four-of long?)

  (select-ids {:account/name '_} *ds*)
  => (eight-of long?)

  (select-ids {:account/tags '_} *ds*)
  => (eight-of long?)

  (select-ids {:account/tags "boo"} *ds*)
  => (eight-of long?)

  (select-ids {:account/tags "sun"} *ds*)
  => (four-of long?)

  (select-ids {:account/tags "moon"} *ds*)
  => (four-of long?)

  (select-ids {:account/name "chris"
               :account/tags "moon"} *ds*)
  => (two-of long?)

  (select-ids {:account/name "chris"
              :account/tags #{"g2" "boo"}} *ds*)
  => (two-of long?)

  (select-ids {:account/name "chris"
               :account/tags #{"g2" "moon"}} *ds*)
  => (one-of long?)

  (select-ids {:account/name #{"dave" "chris"}} *ds*)
  => empty?)


(fact "select-entities"
  (against-background [(before :contents (reset-database))])

  (adi/select-entities {:account {:id '_}} *ds*)
  => (eight-of entity?)

  (select-entities {:account/name "chris"} *ds*)
  => (four-of entity?)

  (select-entities {:account/name '_} *ds*)
  => (eight-of entity?)

  (select-entities {:account/tags '_} *ds*)
  => (eight-of entity?)

  (select-entities {:account/tags "boo"} *ds*)
  => (eight-of entity?)

  (select-entities {:account/tags "sun"} *ds*)
  => (four-of entity?)

  (select-entities {:account/tags "moon"} *ds*)
  => (four-of entity?)

  (select-entities {:account/name "chris"
               :account/tags "moon"} *ds*)
  => (two-of entity?)

  (select-entities {:account/name "chris"
              :account/tags #{"g2" "boo"}} *ds*)
  => (two-of entity?)

  (select-entities {:account/name "chris"
               :account/tags #{"g2" "moon"}} *ds*)
  => (one-of entity?)

  (select-entities {:account/name #{"dave" "chris"}} *ds*)
  => empty?)


(fact "update! commands"
  (against-background
    [(before :checks (reset-database))])

  (select-ids {:account/name "chris"} *ds*)
  => (four-of long?)

  (do
    (update! {:account/name "chris"} {:account/name "adam"} *ds*)
    (select-ids {:account/name "chris"} *ds*))
  => empty?

  (do
    (update! {:account/name "chris"} {:account/name "adam"} *ds*)
    (select-ids {:account/name "adam"} *ds*))
  => (four-of long?)

  (do
    (update! {:account/name "chris"} {:account/tags "another"} *ds*)
    (select-ids {:account/tags "another"} *ds*))
  => (four-of long?))


(fact "retract! commands"
  (against-background
    [(before :checks (reset-database))])

  (select-ids {:account/name "chris"
               :account/tags "boo"} *ds*)
  => (four-of long?)

  (do
    (retract! {:account/name "chris"}
              [[:account/tags "boo"]] *ds*)
    (select-ids {:account/name "chris"} *ds*))
  => (four-of long?)

  (do
    (retract! {:account/name "chris"}
              [[:account/tags "boo"]] *ds*)
    (select-ids {:account/name "chris"
                 :account/tags "boo"} *ds*))
  => empty?


  (do
    (retract! {:account/name "chris"}
              [:account/tags] *ds*)
    (select-ids {:account/name "chris"} *ds*))
  => (four-of long?)

  (do
    (retract! {:account/name "chris"}
              [[:account/tags #{"moon" "g2"}]] *ds*)
    (select {:account/tags #{"moon" "g2"}} *ds*))
  => (one-of (fn [x] (= "dave" (get-in x [:account :name]))))


  (do
    (retract! {:account/name "chris"}
              [:account/tags] *ds*)
    (select-ids {:account/name "chris"
                 :account/tags '_} *ds*))
  => empty?)

(fact "delete! commands"
  (against-background
    [(before :checks (reset-database))])

  (select-ids {:account/name "chris"} *ds*)
  => (four-of long?)

  (do
    (delete! {:account/name "chris"} *ds*)
    (select-ids {:account/name '_} *ds*))
  => (four-of long?)

  (do
    (delete! {:account/tags "g2"} *ds*)
    (select-ids {:account/name "chris"} *ds*))
  => (two-of long?)

  (do
    (delete! {:account/tags "moon"} *ds*)
    (select-ids {:account/name "chris"
                 :account/tags "g2"} *ds*))
  => (one-of long?)

  (do
    (delete! {:account/name '_} *ds*)
    (select-ids {:account/tags '_} *ds*))
  => empty?)
