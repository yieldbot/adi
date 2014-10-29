(ns adi.examples.step-5
  (:use midje.sweet)
  (:require [adi.core :as adi]
            [adi.test.checkers :refer :all]
            [datomic.api :as datomic]))

[[:chapter {:title "Example - Schoolyard"}]]

[[:section {:title "Definition and Setup"}]]

"We want to model a simple school, and we have the standard information like classes, teachers students.
The schema for our bookstore model can be seen in `Figure {{schema-4}}`. It is a rather simplistic
model. This is actually much like the Bookstore example with a couple more fields."

[[:image {:tag "schema-5" :title "Schema Diagram"
          :src "example5.png"}]]

(def schema-5
  {:class   {:type    [{:type :keyword}]
             :name    [{:type :string}]
             :accelerated [{:type :boolean}]
             :teacher [{:type :ref                  ;; <- Note that refs allow a reverse
                        :ref  {:ns   :teacher       ;; look-up to be defined to allow for more
                               :rval :teaches}}]}   ;; natural expression. In this case,
   :teacher {:name     [{:type :string              ;; we say that every `class` has a `teacher`
                         :fulltext true}]
             :canTeach [{:type :keyword             ;; so the reverse will be defined as a
                         :cardinality :many}]       ;; a `teacher` `teaches` a class
             :pets     [{:type :keyword
                         :cardinality :many}]}
   :student {:name     [{:type :string}]
             :siblings [{:type :long}]
             :classes    [{:type :ref
                         :ref   {:ns   :class
                                 :rval :students}   ;; Same with students
                           :cardinality :many}]}})

(facts

  "Once again, the adi datastore is created:"

  (def ds (adi/connect! "datomic:mem://adi-example-step-5" schema-5 true true))

  (comment
    (println ds)
    ;; #adi{:connection #connection{1001 #inst "2014-10-29T19:05:09.697-00:00"}
    ;;      :schema #schema{:teacher {:pets :keyword<*>
    ;;                                :canTeach :keyword<*>
    ;;                                :teaches :&class<*>
    ;;                                :name :string}
    ;;                      :class   {:type :keyword
    ;;                                :name :string
    ;;                                :teacher :&teacher
    ;;                                :students :&student<*>
    ;;                                :accelerated :boolean}
    ;;                      :student {:classes :&class<*>
    ;;                                :name :string
    ;;                                :siblings :long}}}
    )

  "Now here is the fun part. Lets fill in the data. This is one way of filling out the data.
  There are many other ways. Note that it is object-like in nature, with links defined through ids.
  If it doesn't contain an id, the record is automatically created. The example is slightly contrived mainly
  to show-off some different features of `adi`:"

  (def class-data                      ;;; Lets See....
    [{:db/id [[:MATHS]]
      :class {:type :maths             ;;; There's Math. The most important subject
              :name "Maths"            ;;; We will be giving all the classes ids
              :accelerated true}}      ;;; for easier reference

     {:db/id [[:SCIENCE]]              ;;; Lets add Science and Art
      :class {:type :science
              :name "Science"
              :accelerated false}}

     {:db/id [[:ART]]
      :class {:type :art
              :name "Art"
              :accelerated false}}

     {:teacher {:name "Mr. Blair"                       ;; Here's Mr Blair
                :teaches #{[[:ART]]
                           [[:SCIENCE]]}                ;; He also teaches Science
                :canTeach #{:maths :science}
                :pets    #{:fish :bird}}}

     {:teacher {:name "Mr. Carpenter"                   ;; This is Mr Carpenter
                :canTeach #{:sports :maths}
                :pets    #{:dog :fish :bird}
                :teaches #{{:+ {:db/id [[:SPORTS]]}      ;; He teaches sports
                            :type :sports
                            :name "Sports"
                            :accelerated false
                            :students #{{:name "Jack"   ;; There's Jack
                                         :siblings 4    ;; Who is also in EnglishB and Maths
                                         :classes #{{:+ {:db/id [[:ENGLISHB]]}
                                                     :type :english
                                                     :name "English B"
                                                     :accelerated false
                                                     :students #{{:name  "Anna"  ;; There's also Anna in the class
                                                                  :siblings 1
                                                                  :classes #{[[:ART]]}}
                                                                 {:name    "Charlie"
                                                                  :siblings 3
                                                                  :classes  #{[[:ART]]}}
                                                                 {:name    "Francis"
                                                                  :siblings 0
                                                                  :classes #{[[:MATHS]]}}
                                                                 {:name    "Harry"
                                                                  :siblings 2
                                                                  :classes #{[[:SCIENCE]]}}}}}}}}}}}
     {:db/id [[:ENGLISHA]]       ;; What about English A ?
      :class {:type :english
              :name "English A"
              :accelerated true
              :teacher {:name "Mr. Anderson"   ;; Mr Anderson is the teacher
                        :teaches  #{[[:MATHS]]
                                    [[:ENGLISHB]]}   ;; He also takes Maths
                        :canTeach :maths
                        :pets     :dog}
              :students #{{:name "Bobby"   ;; And the students are listed
                           :siblings 2
                           :classes  #{[[:MATHS]]}}
                          {:name "David"
                           :siblings 5
                           :classes #{[[:SCIENCE]]
                                      [[:MATHS]]}}
                          {:name "Erin"
                           :siblings 1
                           :classes #{[[:ART]]}}
                          {:name "Kelly"
                           :siblings 0
                           :classes #{[[:SCIENCE]]
                                      [[:MATHS]]}}}}}])

  "Okay... our data is defined... and..."

  (adi/insert! ds class-data)

  "**BAM!!** We are now ready to do some Analysis"

  [[:section {:title "Datomic"}]]

  "By now, you should be familiar with this query:"

  (adi/select ds {:student/name "Harry"})
  => #{{:student {:name "Harry", :siblings 2}}}

  "What we want to explore is how this query relates to the Datomic API. So lets
  go under the hood a little bit and check out the :db/id of the entity that we
  are gettin back:"

  (comment
    (adi/select ds {:student/name "Harry"} :first :return-ids)
    => 17592186045426)

  "Lets now do a raw datomic query."

  (comment
    (datomic/q '[:find ?x :where
                 [?x :student/name "Harry"]]
               (datomic/db (:connection ds)))

    => #{[17592186045426]})

  "As can be seen, the `select` function is just a more succinct version of `q` with many added
  features."

  [[:section {:title "Querying"}]]

  "There is a `query` method that is halfway between `select` and `q` in terms and
  is convenient for dropping back into datalog queries. We see more examples of the
  query for Harry's id:"

  (comment
    (adi/query ds '[:find ?x :where
                    [?x :student/name "Harry"]]
               []  :first :return-ids)
    => 17592186045426)

  "And again, the same query with `Harry` passed in as a parameter:"

  (comment
    (adi/query ds '[:find ?x
                    :in $ ?name
                    :where
                    [?x :student/name ?name]]
               ["Harry"] :first :return-ids)
    => 17592186045426)

  "Lets do a `query` for all the students in maths:"

  (->> (adi/query ds
                  '[:find ?x :in $ ?class :where
                    [?x :student/classes ?c]
                    [?c :class/type ?class]]
                  [:maths])
       (mapv #(-> % :student :name)))
  => ["Bobby" "Francis" "David" "Kelly"]

  "Here is the equivalent result using `select`"

  (->> (adi/select ds {:student {:classes/type :maths}})
       (mapv #(-> % :student :name)))
  => ["Bobby" "Francis" "David" "Kelly"]

  [[:section {:title "Datalog Generation"}]]

  "Now the cool thing is that `select` actually generates
  a datalog query first and then runs it against datomic. We can
  access the datalog query via the `:raw` option:"

  (comment
    (adi/select  ds {:student {:classes/type :maths}} :raw)

    => #{[:find ?self :where
          [?self :student/classes ?e25242]
          [?e25242 :class/type :maths]]})

  "So very complex queries can be built up"

  (adi/select ds {:student {:classes/teacher {:name '(?fulltext "Anderson")}
                            :siblings '(> 1)}})
  => #{{:student {:name "Bobby", :siblings 2}}
       {:student {:name "David", :siblings 5}}}

  "Lets check out what the generated datalog query is:"

  (comment
    (adi/select ds {:student {:classes/teacher {:name '(?fulltext "Anderson")}
                              :siblings '(> 1)}} :raw)
    => #{[:find ?self :where
          [?self :student/siblings ?e_28962]
          [(> ?e_28962 1)]
          [?self :student/classes ?e28960]
          [?e28960 :class/teacher ?e28961]
          [(fulltext $ :teacher/name "Anderson")
           [[?e28961 ?e_28963]]]]})

  "As can be seen by this example, the `adi` query is much much more succinct. We can
  now take the output and stick it into `query` to get the same result as before:"

  (adi/query ds '[:find ?self :where
                  [?self :student/siblings ?e_28962]
                  [(> ?e_28962 1)]
                  [?self :student/classes ?e28960]
                  [?e28960 :class/teacher ?e28961]
                  [(fulltext $ :teacher/name "Anderson")
                   [[?e28961 ?e_28963]]]]
             [])
  => #{{:student {:name "Bobby", :siblings 2}}
       {:student {:name "David", :siblings 5}}}

  "So which one will you prefer to be using?"

  [[:section {:title "Expressivity"}]]

  "Find the teacher that teaches a student called `Harry` with a pet bird"
  (->> (adi/select ds {:teacher {:teaches {:students/name "Harry"}
                                 :pets :bird}})
       (mapv #(-> % :teacher :name)))
  => ["Mr. Blair"]

  "Find all students taught by `Mr. Anderson`:"
  (->>
   (adi/select ds {:student/classes {:teacher/name "Mr. Anderson"}})
   (mapv #(-> % :student :name)))
  => ["Bobby" "Francis" "David" "Erin" "Kelly"]

  "Find all the students that have class with teachers with a pet fish"
  (->>
   (adi/select ds {:student/classes {:teacher/pets :fish}})
   (mapv #(-> % :student :name)))
  => ["Charlie" "Jack" "Anna" "David" "Harry" "Erin" "Kelly"]

  "Not that you'd ever want to write a query like this but you can. Find the class with the teacher that
  teaches a student that takes the class taken by `Mr. Carpenter`."
  (->>
   (adi/select ds {:class/teacher
                   {:teaches/students
                    {:classes/teacher {:name "Mr. Carpenter"}}}})
   (mapv #(-> % :class :name)))
  => ["Sports"]

  "Note that the search path `:class/teacher/teaches/students/classes/teacher/name` will also work.")
  
(future-fact
  (adi/select ds {:class/teacher {:name "Mr. Anderson"}})
  => #{{:class {:type :english, :name "English A", :accelerated true}}
       {:class {:type :english, :name "English B", :accelerated false}}
       {:class {:type :maths, :name "Maths", :accelerated true}}})
