(ns adi.data.school
  (:use [adi.utils :only [iid ?q]])
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
    :class {:type :sports
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
                                  {:+/db/id (iid :Sports)}}}
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
                       :classes #{{:+/db/id (iid :Sports)}
                                  {:+/db/id (iid :Maths)}}}}}])

(adi/insert! class-data class-datastore)

### Selecting


;; Find the student with the name Harry

(adi/select {:student/name "Harry"} class-datastore) ;=> Returns a map with Harry

(->> ;; Find the student that takes sports
 (adi/select {:student/classes/type :sports} class-datastore)
 (map #(-> % :student :name))) ;=> ("Ivan" "Anna" "Jack")


(->> ;; Find the teacher that teaches a student called Harry
 (adi/select {:teacher/teaches/students/name "Harry"} class-datastore)
 (map #(-> % :teacher :name))) ;=> ("Mr. Anderson" "Mr. Carpenter" "Mr. Blair")


(->> ;; Find students that have less than 2 siblings and take art
 (adi/select {:student {:siblings (?q < 2)
                        :classes/type :art}} class-datastore)
 (map #(-> % :student :name))) ;=> ("Erin" "Anna" "Francis")


(->> ;; Find the classes that Mr Anderson teaches David
 (adi/select {:class {:teacher/name "Mr. Anderson"
                      :students/name "David"}} class-datastore)
 (map #(-> % :class :name))) ;=> ("English A" "Maths")


### Updating

(-> ;; Find the number of siblings Harry has
 (adi/select {:student/name "Harry"} class-datastore)
 first :student :siblings) ;=> 2

(-> ;; His mum just had twins!
 (adi/update! {:student/name "Harry"} {:student/siblings 4} class-datastore))

(-> ;; Find the number of siblings Harry has
 (adi/select {:student/name "Harry"} class-datastore)
 first :student :siblings) ;=> 4

(->> ;; Find all the students that have class with teachers with fish
 (adi/select {:student/classes/teacher/pets :fish } class-datastore)
 (map #(-> % :student :name)) sort)
;=> ("Anna" "Charlie" "David" "Francis" "Harry" "Ivan" "Jack" "Kelly")


## Retractions

(->> ;; Find all the students that have class with teachers with dogs
 (adi/select {:student/classes/teacher/pets :dog} class-datastore)
 (map #(-> % :student :name))
 sort)
;=> ("Anna" "Bobby" "Charlie" "David" "Erin" "Francis" "Harry" "Ivan" "Jack" "Kelly")

;;That teacher who teaches english-a's dog just died
(adi/retract! {:teacher/teaches/name "English A"}
              {:teacher/pets :dog} class-datastore)
(->> ;; Find all the students that have class with teachers with dogs
 (adi/select {:student/classes/teacher/pets :dog} class-datastore)
 (map #(-> % :student :name))
 sort)
;;=> ("Anna" "Charlie" "Francis" "Harry" "Ivan" "Jack")


### Deletions
(->>
 (adi/select {:student/classes/type :sports} class-datastore)
 (map #(-> % :student :name)))
;=> ("Ivan" "Anna" "Jack")


;; Ivan went to another school
(adi/delete! {:student/name "Ivan"} class-datastore)

(->> ;; See who is left in the sports class
 (adi/select {:student/classes/type :sports} class-datastore)
 (map #(-> % :student :name)))
;=> ("Anna" "Jack")

;; The students in english A had a bus accident
(adi/delete! {:student/classes/name "English A"} class-datastore)

(->>
 (adi/select :student/name class-datastore)
 (map #(-> % :student :name)))
;=> ("Anna" "Charlie" "Francis" "Jack" "Harry")
