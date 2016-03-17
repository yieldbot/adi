(ns documentation.adi-guide.options.query
  (:use midje.sweet)
  (:require [adi.core :as adi]))

"For demonstration of search options, we define a schema"

(def schema-school
  {:student   {:name      [{:required true
                            :fulltext true}]
               :classes   [{:type :ref
                            :cardinality :many
                            :ref {:ns   :class}}]}
   :class     {:subject [{:required true}]
               :teacher [{:type :ref
                          :ref {:ns   :teacher
                                :rval :class}}]}
   :teacher   {:name    [{:required true}]
               :age     [{:type :long}]}})

"The datastore is created:"

(def school-ds (adi/connect! "datomic:mem://adi-guide-options-school-store" schema-school true true))

"Data is put into the system:"

(adi/insert! school-ds
             [{:db/id (adi/iid :science)
               :class {:subject "Science"
                       :teacher {:name "Mr Michaels"
                                 :age 39}}}
              {:db/id (adi/iid :math)
               :class {:subject "Math"
                       :teacher {:name "Mr Nolan"
                                 :age 26}}}
              {:student {:name "Charlie" :classes #{(adi/iid :science)}}}
              {:student {:name "Bob"     :classes #{(adi/iid :math)}}}
              {:student {:name "Anne"    :classes #{(adi/iid :science)
                                                    (adi/iid :math)}}}])

"And we are ready to go!"

[[:section {:title ":first"}]]

"Instead of returning a set of values, returns the first element of the results. Useful when only one value is returned:"

(fact
  (adi/select school-ds {:student/name "Anne"} {:options {:first :true}})
  => {:student {:name "Anne"}})

"The shorthand can be used:"

(fact
  (adi/select school-ds {:student/name "Anne"} :first)
  => {:student {:name "Anne"}})

"Also works when there is more than one possible value:"

(fact
  (adi/select school-ds :student)
  => #{{:student {:name "Bob"}} {:student {:name "Anne"}} {:student {:name "Charlie"}}})

"Because `Charlie` was put in first, we expect that his name comes up first:"

(fact
  (adi/select school-ds :student :first)
  => {:student {:name "Charlie"}})

[[:section {:title ":ids"}]]

"Returns the datomic ids for the piece of data:"

(fact
  (adi/select school-ds {:student/name "Anne"} {:options {:ids :true}})

  => #{{:student {:name "Anne"}, :db {:id 17592186045424}}})

"`:ids` work fine with `:pull`, `:access` and data related options"

(fact
  (adi/select school-ds {:student/name "Anne"}
              :ids
              :pull {:student {:classes {:teacher :checked}}})
  
  => #{{:db {:id 17592186045424}
        :student {:name "Anne",
                  :classes #{{:+ {:db {:id 17592186045420}}
                              :subject "Math",
                              :teacher {:+ {:db {:id 17592186045421}}
                                        :name "Mr Nolan"
                                        :age 26}}
                             {:+ {:db {:id 17592186045418}}
                              :subject "Science",
                              :teacher {:+ {:db {:id 17592186045419}}
                                        :name "Mr Michaels"
                                        :age 39}}}}}})

[[:section {:title ":ban-expressions"}]]

"expressions can be used to select"

(fact
  (adi/select school-ds {:student/name '(.startsWith ? "A")})
  => #{{:student {:name "Anne"}}})

"At times, especially when directly exposing the interface to the outside, it may be a good idea to disable this functionality:"

(fact
  (adi/select school-ds {:student/name '(.startsWith ? "A")}
              :ban-expressions)
  => (throws))

[[:section {:title ":ban-top-id"}]]

"When there are no restrictions, ids can be used as direct input:"

(fact
  (adi/select school-ds 17592186045424)
  => #{{:student {:name "Anne"}}})

"This option disables selection of entity from the very top"

(fact
  (adi/select school-ds 17592186045424 :ban-top-id)
  => (throws))

"However, this does not prevent selection of related data:"

(fact
  (adi/select school-ds {:class/students 17592186045424} :ban-top-id)
  => #{{:class {:subject "Math"}} {:class {:subject "Science"}}})

[[:section {:title ":ban-body-ids"}]]

"The opposite of `:ban-top-id`. This option disables selection of entity from the query:"

(fact
  (adi/select school-ds 17592186045424 :ban-body-ids)
  #{{:student {:name "Anne"}}})

"And when numbers are not in the body, an exception is thrown:"

(fact
  (adi/select school-ds {:class/students 17592186045424} :ban-body-ids)
  => (throws))

[[:section {:title ":ban-ids"}]]

"The combination of both options. Throws whever it sees an id:"

(fact
  (adi/select school-ds 17592186045424 :ban-ids)
  => (throws))

"And when numbers are not in the body, an exception is thrown:"

(fact
  (adi/select school-ds {:class/students 17592186045424} :ban-ids)
  => (throws))

[[:section {:title ":use-typecheck"}]]

""
(fact
  (adi/select school-ds {:student/classes/teacher/age "39"})
  => #{{:student {:name "Anne"}} {:student {:name "Charlie"}}})

(fact
  (adi/select school-ds {:student/classes/teacher/age "38"}
              :use-typecheck)
  => (throws))

[[:section {:title ":use-coerce"}]]

(fact
  (adi/select school-ds {:student/classes/teacher/age "39"}
              :use-typecheck
              :use-coerce)
  => #{{:student {:name "Anne"}} {:student {:name "Charlie"}}})

