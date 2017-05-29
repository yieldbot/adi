(ns spirit.core.bug-fixes
  (:use hara.test)
  (:require [spirit.core :as spirit]
            [spirit.test.checkers :refer :all]
            [datomic.api :as datomic]))

(def schema
  {:class   {:subject    [{:type :keyword}]
             :teacher    [{:type :ref                  ;; <- Note that refs allow a reverse
                           :ref  {:ns   :teacher       ;; look-up to be defined to allow for more
                                  :rval :teaches}}]}   ;; natural expression. In this case,
   :teacher {:name       [{:type :string               ;; we say that every `class` has a `teacher`
                          :fulltext true}]}
   :student {:name       [{:type :string}]
             :classes    [{:type :ref
                           :ref   {:ns   :class
                                   :rval :students}   ;; Same with students
                           :cardinality :many}]}})

(do (def ds (spirit/connect! "datomic:mem://spirit-bug-fix" schema true true))

    (spirit/insert! ds {:db/id [[:JACK]]
                     :student {:name "Jack"
                               :classes #{{:+/db/id [[:ENGLISHB]]
                                           :subject :english}}}}))

(spirit/select ds #{{:student/name "Jack"}})
(spirit/insert! ds [{:db/id [[:MATHS]]
                  :class {:subject  :maths}}

                 {:db/id [[:JACK]]
                  :student {:name "Jack"
                            :classes #{[[:MATHS]]
                                       {:subject :english}}}}])


(do (def ds (spirit/connect! "datomic:mem://spirit-bug-fix" schema true true))
    (spirit/insert! ds {:db/id [[:CARPENTER]]
                     :teacher {:name "Mr. Carpenter"                   ;; This is Mr Carpenter
                               :teaches #{{:+ {:db/id [[:SPORTS]]}      ;; He teaches sports
                                           :subject :sports
                                           :students #{{:+ {:db/id [[:JACK]]}
                                                        :name "Jack"   ;; There's Jack
                                                        :classes #{{;;:+ {:db/id [[:ENGLISHB]]}
                                                                    :subject :english}}}}}}}}))
