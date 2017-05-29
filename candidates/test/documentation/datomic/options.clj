(ns documentation.datomic.options
  (:use hara.test)
  (:require [spirit.core :as spirit]))

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

(def school-ds (spirit/connect! "datomic:mem://datomic-options-school-store" schema-school true true))

"Data is put into the system:"

(spirit/insert! school-ds
             [{:db/id (spirit/iid :science)
               :class {:subject "Science"
                       :teacher {:name "Mr Michaels"
                                 :age 39}}}
              {:db/id (spirit/iid :math)
               :class {:subject "Math"
                       :teacher {:name "Mr Nolan"
                                 :age 26}}}
              {:student {:name "Charlie"
                         :classes #{(spirit/iid :science)}}}
              {:student {:name "Bob"
                         :classes #{(spirit/iid :math)}}}
              {:student {:name "Anne"
                         :classes #{(spirit/iid :science)
                                    (spirit/iid :math)}}}])

"And we are ready to go!"

[[:section {:title ":first"}]]

"Instead of returning a set of values, returns the first element of the results. Useful when only one value is returned:"

(fact
  (spirit/select school-ds {:student/name "Anne"} {:options {:first :true}})
  => {:student {:name "Anne"}})

"The shorthand can be used:"

(fact
  (spirit/select school-ds {:student/name "Anne"} :first)
  => {:student {:name "Anne"}})

"Also works when there is more than one possible value:"

(fact
  (spirit/select school-ds :student)
  => #{{:student {:name "Bob"}} {:student {:name "Anne"}} {:student {:name "Charlie"}}})

"Because `Charlie` was put in first, we expect that his name comes up first:"

(fact
  (spirit/select school-ds :student :first)
  => {:student {:name "Charlie"}})

[[:section {:title ":ids"}]]

"Returns the datomic ids for the piece of data:"

(fact
  (spirit/select school-ds {:student/name "Anne"} {:options {:ids :true}})

  => #{{:student {:name "Anne"}, :db {:id 17592186045424}}})

"`:ids` work fine with `:pull`, `:access` and data related options"

(fact
 (spirit/select school-ds {:student/name "Anne"}
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

[[:section {:title ":blank"}]]

"works with `:pull` to specify how refs should be returned:"

(fact
 (spirit/select school-ds {:student/name "Anne"} :blank)
 => #{{}})

[[:subsection {:title "default setting"}]]

"For default behaviour on queries, this option can be set as default on initialisation:"

(def blank-ds
  (spirit/connect! {:uri "datomic:mem://datomic-options-school-store"
                 :schema schema-school
                 :options {:blank true}}))


(fact
  (spirit/select blank-ds {:student/name "Anne"})
  => #{{}})

[[:subsection {:title "pull behaviour"}]]

"Once set, the results will only be what the model governed by `:pull` allows:"

(fact
 (spirit/select blank-ds {:student/name "Anne"}
             :pull {:student {:name :checked}})
 => #{{:student {:name "Anne"}}})

(fact
 (spirit/select blank-ds {:student/classes {:teacher/age 39}}
             :pull {:student {:name :checked}})
 => #{{:student {:name "Anne"}}
      {:student {:name "Charlie"}}})

[[:subsection {:title "access behaviour"}]]

"`:access` will also work the same as `:pull` but with query restrictions"
(fact
  (spirit/select blank-ds {:student/name "Anne"}
              :first
              :access {:student {:name :checked}})
  => {:student {:name "Anne"}})

(fact
  (spirit/select blank-ds {:student/classes {:teacher/age 39}}
              :access {:student {:name :checked}})
  => (throws))

[[:subsection {:title "ref traversal"}]]

"when a key in a ref is checked, it will return only the data for that particular key"

(fact
  (spirit/select school-ds {:student/name "Anne"}
              :blank
              :pull {:student {:classes {:teacher {:name :checked}}}})
  => #{{:student {:classes #{{:teacher {:name "Mr Michaels"}}
                             {:teacher {:name "Mr Nolan"}}}}}})

"when the ref itself is checked, it will return all non ref data"

(fact
 (spirit/select school-ds {:student/name "Anne"}
             {:options {:blank true}
              :pull {:student {:classes {:teacher :checked}}}})
 => #{{:student {:classes #{{:teacher {:name "Mr Michaels", :age 39}}
                            {:teacher {:name "Mr Nolan", :age 26}}}}}})


[[:section {:title ":ban-expressions"}]]

"expressions can be used to select"

(fact
 (spirit/select school-ds {:student/name '(.startsWith ? "A")})
 => #{{:student {:name "Anne"}}})

"At times, especially when directly exposing the interface to the outside, it may be a good idea to disable this functionality:"

(fact
  (spirit/select school-ds {:student/name '(.startsWith ? "A")}
              :ban-expressions)
  => (throws))

[[:section {:title ":ban-top-id"}]]

"When there are no restrictions, ids can be used as direct input:"

(fact
  (spirit/select school-ds 17592186045424)
  => #{{:student {:name "Anne"}}})

"This option disables selection of entity from the very top"

(fact
  (spirit/select school-ds 17592186045424 :ban-top-id)
  => (throws))

"However, this does not prevent selection of related data:"

(fact
  (spirit/select school-ds {:class/students 17592186045424} :ban-top-id)
  => #{{:class {:subject "Math"}} {:class {:subject "Science"}}})

[[:section {:title ":ban-body-ids"}]]

"The opposite of `:ban-top-id`. This option disables selection of entity from the query:"

(fact
  (spirit/select school-ds 17592186045424 :ban-body-ids)
  #{{:student {:name "Anne"}}})

"And when numbers are not in the body, an exception is thrown:"

(fact
  (spirit/select school-ds
              {:class/students 17592186045424} :ban-body-ids)
  => (throws))

[[:section {:title ":ban-ids"}]]

"The combination of both options. Throws whever it sees an id:"

(fact
  (spirit/select school-ds 17592186045424 :ban-ids)
  => (throws))

"And when numbers are not in the body, an exception is thrown:"

(fact
  (spirit/select school-ds
              {:class/students 17592186045424} :ban-ids)
  => (throws))

[[:section {:title ":raw"}]]

"Returns the actual input that would be given to datomic:"

(spirit/select school-ds
            {:class/students 17592186045424} :raw)
;;=> [:find ?self :where [17592186045424 :student/classes ?self]]

"Works for both queries and datoms:"

(comment
  (spirit/insert! school-ds
               [{:db/id (spirit/iid :science)
                 :class {:subject "Science"
                         :teacher {:name "Mr Michaels"
                                   :age 39}}}
                {:db/id (spirit/iid :math)
                 :class {:subject "Math"
                         :teacher {:name "Mr Nolan"
                                   :age 26}}}
                {:student {:name "Charlie"
                           :classes #{(spirit/iid :science)}}}
                {:student {:name "Bob"
                           :classes #{(spirit/iid :math)}}}
                {:student {:name "Anne"
                           :classes #{(spirit/iid :science)
                                      (spirit/iid :math)}}}]
               :raw)
  ;;=> [{:db/id #spirit[:science]
  ;;     :class/subject "Science"}
  ;;     :class/teacher {:db/id #spirit[?e262028]
  ;;                     :teacher/name "Mr Michaels"
  ;;                     :teacher/age 39}} 
  ;;    {:db/id #spirit[:math]
  ;;     :class/subject "Math"
  ;;     :class/teacher {:db/id #spirit[?e262029]
  ;;                     :teacher/name "Mr Nolan"
  ;;                     :teacher/age 26}}
  ;;    {:student/name "Charlie", :db/id #spirit[?e262030]}
  ;;    [:db/add #spirit[?e262030] :student/classes #spirit[:science]]
  ;;    {:student/name "Bob", :db/id #spirit[?e262031]}
  ;;    [:db/add #spirit[?e262031] :student/classes #spirit[:math]]
  ;;    {:student/name "Anne", :db/id #spirit[?e262032]}
  ;;    [:db/add #spirit[?e262032] :student/classes #spirit[:science]]
  ;;    [:db/add #spirit[?e262032] :student/classes #spirit[:math]])
)

[[:section {:title ":spirit"}]]

"Returns the `spirit` datastructure used for the query, useful for debugging. This map has all the acculmulated data as the query/insert moves through the spirit data pipeline"

(comment
  (spirit/select school-ds {:class/students 17592186045424} :spirit)
  ;;=> #spirit{:tempids {:status :ready, :val #{}},
  ;;        :schema #schema{:student {:name :string,
  ;;                                  :classes :&class<*>},
  ;;                        :class   {:subject :string,
  ;;                                  :teacher :&teacher,
  ;;                                  :students :&student<*>},
  ;;                        :teacher {:name :string,
  ;;                                  :age :long,
  ;;                                  :class :&class<*>}}
  ;;        :pipeline nil,
  ;;        :db #db{1001 #inst "2016-03-18T06:14:23.156-00:00"},
  ;;        :process {:input
  ;;                  {:class/students 17592186045424},
  ;;
  ;;                  :normalised
  ;;                  {:class {:students #{17592186045424}}}
  ;;
  ;;                  :analysed
  ;;                  {:# {:sym ?self, :id ?e262444}
  ;;                   :class/students #{17592186045424}}
  ;;
  ;;                  :reviewed
  ;;                  {:# {:sym ?self, :id ?e262444}
  ;;                   :class/students #{17592186045424}}
  ;;
  ;;                  :characterised
  ;;                  {:# {:sym ?self, :id ?e262444}
  ;;                   :rev-ids-many {:student/classes #{}}
  ;;                   :rev-ids {:student/classes #{17592186045424}}
  ;;
  ;;                  :emitted
  ;;                  [:find ?self :where
  ;;                   [17592186045424 :student/classes ?self]]},
  ;;        :type "query",
  ;;        :op :select,
  ;;        :result {:ids (17592186045418 17592186045420),
  ;;                 :entities ({:db/id 17592186045418}
  ;;                            {:db/id 17592186045420}),
  ;;                 :data ({:class {:subject "Science"}}
  ;;                        {:class {:subject "Math"}})}
  ;;        :options {:spirit true},
  ;;        :connection #connection{1001 #inst "2016-03-18T06:14:23.156-00:00"}}
  )
