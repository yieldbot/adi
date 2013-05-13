(ns adi.data.school
  (:use [adi.utils :only [iid]])
  (:require [adi.core :as adi]))


(def class-schema
  {:class   {:type    [{:type :keyword}]
             :name    [{:type :string}]
             :accelerated [{:type :boolean}]
             :teacher [{:type :ref
                        :ref  {:ns   :teacher
                               :rval :teaches}}]}
   :teacher {:name     [{:type :string}]
             :canTeach [{:type :keyword
                         :cardinality :many}]
             :pets     [{:type :keyword
                         :cardinality :many}]}
   :student {:name     [{:type :string}]
             :siblings [{:type :long}]
             :classes    [{:type :ref
                         :ref   {:ns   :class
                                 :rval :students}
                         :cardinality :many}]}})

(def class-datastore
  (adi/datastore "datomic:mem://adi-class-datastore" class-schema true true))

(def class-data
  [{:db/id (iid :EnglishA)
    :class {:type :english
            :name "English A"
            :teacher {:name "Mr. Anderson"
                      :teaches  {:+/db/id (iid :Maths)}
                      :canTeach :maths
                      :pets     :dog}}}

   {:db/id (iid :EnglishB)
    :class {:type :english
            :name "English B"
            :teacher {:name "Mr. Carpenter"
                      :canTeach #{:sports :maths}
                      :teaches {:+/db/id (iid :Sports)}
                      :pets    #{:dog :fish :bird}}}}
   {:db/id (iid :Sports)
    :class {:type :english
            :name "Sports"
            :accelerated false}}

   {:db/id (iid :Maths)
    :class {:type :maths
            :name "Maths"
            :accelerated true}}

   {:db/id (iid :Art)
    :class {:type :art
            :name "Art"
            :accelerated true}}

   {:db/id (iid :Science)
    :class {:type :science
            :name "Science"
            :teacher {:name "Mr. Blair"
                      :teaches {:+/db/id (iid :Science)}
                      :canTeach #{:maths :science}
                      :pets    #{:fish :bird}}}}

   {:db/id (iid :EnglishA)
    :class/students #{{:name "Bobby"
                       :siblings 2
                       :classes  {:+/db/id (iid :Maths)}}
                      {:name "David"
                       :siblings 5
                       :classes #{{:+/db/id (iid :Science)}
                                  {:+/db/id (iid :Maths)}}}
                      {:name "Erin"
                       :siblings 1
                       :classes #{{:+/db/id (iid :Art)}}}
                      {:name "Ivan"
                       :siblings 2
                       :classes #{{:+/db/id (iid :Science)}
                                  {:+/db/id (iid :Art)}}}
                      {:name "Kelly"
                       :siblings 0
                       :classes #{{:+/db/id (iid :Science)}
                                  {:+/db/id (iid :Maths)}}}}}
   {:db/id (iid :EnglishB)
    :class/students #{{:name  "Anna"
                       :siblings 1
                       :classes #{{:+/db/id (iid :Sports)}
                                  {:+/db/id (iid :Art)}}}
                      {:name    "Charlie"
                       :siblings 3
                       :classes {:+/db/id (iid :Art)}}
                      {:name    "Francis"
                       :siblings 0
                       :classes #{{:+/db/id (iid :Art)}
                                  {:+/db/id (iid :Maths)}}}
                      {:name    "Harry"
                       :siblings 2
                       :classes #{{:+/db/id (iid :Art)}
                                  {:+/db/id (iid :Science)}
                                  {:+/db/id (iid :Maths)}}}
                      {:name    "Jack"
                       :siblings 4
                       :classes #{{:+/db/id (iid :Art)}
                                  {:+/db/id (iid :Maths)}}}}}])

(adi/insert! class-data class-datastore)


(adi/select {:student/name "Harry"} class-datastore :view {:student/classes :show
                                                           :class/teacher :hide})
({:student {:classes #{{:+ {:db {:id 17592186045420}},
                        :name "Maths", :accelerated true, :type :maths}
                       {:+ {:db {:id 17592186045424}},
                        :name "Art", :accelerated true, :type :art}
                       {:+ {:db {:id 17592186045422}},
                        :name "English B", :type :english}
                       {:+ {:db {:id 17592186045426}},
                        :name "Science", :type :science}},
            :name "Harry", :siblings 2}, :db {:id 17592186045432}})

({:student {:classes #{{:+ {:db {:id 17592186045420}},
                        :name "Maths", :accelerated true, :type :maths}
                       {:+ {:db {:id 17592186045424}},
                        :name "Art", :accelerated true, :type :art}
                       {:+ {:db {:id 17592186045422}}, :name "English B", :type :english} {:+ {:db {:id 17592186045426}}, :name "Science", :type :science}}, :name "Harry", :siblings 2}, :db {:id 17592186045432}})
({:student {:classes #{{:+ {:db {:id 17592186045424}},
                        :name "Art", :accelerated true, :type :art}
                       {:+ {:db {:id 17592186045420}},
                        :teacher {:+ {:db {:id 17592186045418}}},
                        :name "Maths", :accelerated true, :type :maths}
                       {:+ {:db {:id 17592186045422}},
                        :teacher {:+ {:db {:id 17592186045421}}},
                        :name "English B", :type :english}
                       {:+ {:db {:id 17592186045426}},
                        :teacher {:+ {:db {:id 17592186045425}}},
                        :name "Science", :type :science}},
            :name "Harry", :siblings 2}, :db {:id 17592186045432}})


(adi/select {:teacher/teaches/students/name "Harry"} class-datastore)
=> ({:teacher {:name "Mr. Anderson",
            :canTeach #{:maths},
            :pets #{:dog}},
     :db {:id 17592186045418}}
    {:teacher {:name "Mr. Carpenter",
               :canTeach #{:sports :maths},
               :pets #{:dog :fish :bird}},
     :db {:id 17592186045421}}
    {:teacher {:name "Mr. Blair",
               :canTeach #{:science :maths},
               :pets #{:fish :bird}},
     :db {:id 17592186045425}})

(adi/select {:class/teacher/name "Mr. Anderson"} class-datastore)
