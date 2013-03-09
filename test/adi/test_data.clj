(ns adi.test-data
  (:use midje.sweet
        adi.utils
        adi.checkers)
  (:require [adi.data :as ad]))

(fact "correct-type? picks out"
  (ad/correct-type? {:type :string} "data") => true
  (ad/correct-type? {:type :long} 0) => true
  (ad/correct-type? {:type :keyword} :data) => true
  (ad/correct-type? {:type :bigint} 0N) => true
  (ad/correct-type? {:type :bigdec} (bigdec 0)) => true
  (ad/correct-type? {:type :string} "data") => true
  (ad/correct-type? {:type :bytes} (byte-array 10)) => true
  (ad/correct-type? {:type :ref} {:name "data"}) => true
  (ad/correct-type? {:type :uri} (java.net.URI. "http://www.google.com")) => true
  (ad/correct-type? {:type :instant} (java.util.Date.)) => true

  ;; Exceptions
  (ad/correct-type? {:type :string} :NOT-STRING) => (throws Exception))


#_(fact "find-nskv will return the key sequence that gives the value of the map"
  (ad/find-nskv :a/b {}) => nil
  (ad/find-nskv :a/b {:a/b 1}) => [:a/b]
  (ad/find-nskv :a/b {:+ {:a {:b 1}}}) => [:+ :a :b]
  (ad/find-nskv :a/b {:+ {:a/b 1}}) => [:+ :a/b]
  (ad/find-nskv :a/b {:a {:b 1}}) => [:a :b])

#_(fact "find-db-id will return the the datomic id for different data types"
  (ad/find-db-id {}) => nil
  (ad/find-db-id 1) => 1
  (ad/find-db-id {:db {:id 1}}) => 1
  (ad/find-db-id {:db/id 1}) => 1
  (ad/find-db-id {:db {:id 1}}) => 1
  (ad/find-db-id {:+/db/id 1}) => 1
  (ad/find-db-id {:+ {:db/id 1}}) => 1
  (ad/find-db-id {:+ {:db {:id 1}}}) => 1)

(fact "correct-value takes a meta type, does a check to see if
       the data is right and converts it into the right data"
  (ad/correct-value {:type :string} "hello" {})
  => "hello"
  (ad/correct-value {:type :string} "hello" {:sets-only? true})
  => #{"hello"}
  (ad/correct-value {:type :string} #{"there" "hello"} {:sets-only? true})
  => #{"hello" "there"}
  (ad/correct-value {:type :string} #{"there" "hello"} {})
  => (throws Exception)
  (ad/correct-value {:type :string
                     :cardinality :many} #{"there" "hello"} {})
  => #{"hello" "there"}

  (ad/correct-value {:type :string
                     :cardinality :many} "hello" {})
  => #{"hello"}

  (ad/correct-value {:type :string
                     :cardinality :many} #{"there" :hello} {})
  => (throws Exception)

  (ad/correct-value {:type :string
                     :cardinality :many} #{"there" :hello} {:use-sets true})
  => (throws Exception)

  (ad/correct-value {:type :string
                     :cardinality :many} :hello {:use-sets true})
  => (throws Exception))
