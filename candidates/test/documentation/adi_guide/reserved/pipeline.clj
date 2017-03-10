(ns documentation.adi-guide.reserved.pipeline
  (:use midje.sweet)
  (:require [adi.core :as adi]
            [documentation.adi-guide.options :refer :all]))



[[:section {:title ":pre-process"}]]

"Manipulation of the datastructure before going into the pipeline. Takes one function the first the data structure"

(fact
  (adi/select school-ds
              {:student/name "Charlie"}
              {:pipeline {:pre-process
                          (fn [x]
                            (assoc-in x [:student :name] "Bob"))}})
  => #{{:student {:name "Bob"}}})

"Different options can be passed into the datastore through a second optional param: the datastore object:"

(fact
  (adi/select school-ds
              {:student/name "Charlie"}
              {:pipeline {:pre-process
                          (fn [x adi]
                            (assoc-in x [:student :name]
                                      (-> adi :params :name)))}
               :params {:name "Bob"}})
  => #{{:student {:name "Bob"}}})

[[:section {:title ":pre-require"}]]

"Checks to see if data in a certain field is availiable:"

(fact
  (adi/select school-ds
              {:student/name "Charlie"}
              {:pipeline {:pre-require
                          {:student {:name :checked}}}})
  => #{{:student {:name "Charlie"}}})

"If it is not, then the operation throws an error:"

(fact
  (adi/select school-ds
              {:student/classes/subject "Math"}
              {:pipeline {:pre-require
                          {:student {:name :checked}}}})
  => (throws))

"The fields are accessed in a nested fashion:"

(fact
  (adi/select school-ds
              {:student/classes/subject "Math"}
              {:pipeline {:pre-require
                          {:student
                           {:classes {:subject (fn [_ _] true)}}}}})
  => #{{:student {:name "Bob"}}
       {:student {:name "Anne"}}})

[[:section {:title ":pre-mask"}]]

"Removes an entry in the map. For example, it is currently the case that a search for student named Charlie that takes Math class does not yield any results:"

(fact
  (adi/select school-ds
              {:student {:name "Charlie"
                         :classes {:subject "Math"}}})
  => #{})

"However, we can mask the data, in this case, taking out the value for `:student/name` to only search for students that are taking Math class."

(fact
  (adi/select school-ds
              {:student {:name "Charlie"
                         :classes {:subject "Math"}}}
              {:pipeline {:pre-mask
                          {:student {:name :checked}}}})
  => #{{:student {:name "Bob"}}
       {:student {:name "Anne"}}})

"This example shows the class subject being masked:"

(fact
  (adi/select school-ds
              {:student {:name "Charlie"
                         :classes {:subject "Math"}}}
              {:pipeline {:pre-mask
                          {:student
                           {:classes {:subject (fn [_ _] true)}}}}})
  => #{{:student {:name "Charlie"}}})

[[:section {:title ":pre-transform"}]]

"Applies a transformation directly to the value, takes a function containing the value as well as the adi datastructure:"

(defn capitalise [x]
  (str (.toUpperCase (str (first x)))
       (.toLowerCase (apply str (rest x)))))

"Takes one or two arguments, like `pre-process`:"

(fact
  (adi/select school-ds
              {:student {:name "Charlie"
                         :classes {:subject "SCIENCE"}}}
              {:pipeline {:pre-transform
                          {:student
                           {:classes
                            {:subject capitalise}}}}})
  => #{{:student {:name "Charlie"}}})

"A more explicit version where capitalise is passed in is shown:"

(fact
  (adi/select school-ds
              {:student {:name "Charlie"
                         :classes {:subject "SCIENCE"}}}
              {:fn capitalise
               :pipeline {:pre-transform
                          {:student
                           {:classes
                            {:subject
                             (fn [x adi]
                               ((:fn adi) x))}}}}})
  => #{{:student {:name "Charlie"}}})

[[:section {:title ":fill-empty"}]]

"Makes sure that if a value is empty, then it will be filled either by a value"

(fact
  (adi/select school-ds
              {}
              {:pipeline {:fill-empty
                          {:student {:classes
                                     {:subject "Science"}}}}})
  => #{{:student {:name "Anne"}} {:student {:name "Charlie"}}})

"Or a function:"

(fact
  (adi/select school-ds
              {}
              {:params {:subject "Science"}
               :pipeline {:fill-empty
                          {:student
                           {:classes
                            {:subject (fn [x]
                                        "Science")}}}}})
  => #{{:student {:name "Anne"}} {:student {:name "Charlie"}}})

"Again, like previous pipeline segments values can be passed in:"

(fact
  (adi/select school-ds
              {}
              {:params {:subject "Science"}
               :pipeline {:fill-empty
                          {:student
                           {:classes
                            {:subject
                             (fn [_ adi]
                               (-> adi :params :subject))}}}}})
  => #{{:student {:name "Anne"}} {:student {:name "Charlie"}}})

[[:section {:title ":fill-assoc"}]]

"Makes sure that additional values are added, in this case, we have imposed an additional restriction that the student should be taking Math as well as Science:"

(fact
  (adi/select school-ds
              {:student {:classes
                         {:subject "Science"}}}
              {:pipeline {:fill-assoc
                          {:student
                           {:classes #{{:subject "Math"}}}}}})
  => #{{:student {:name "Anne"}}})

"Use of a function is also valid:"

(fact
  (adi/select school-ds
              {:student {:classes
                         {:subject "Science"}}}
              {:pipeline {:fill-assoc
                          {:student
                           {:classes
                            (fn [_] {:subject "Math"})}}}})
  => #{{:student {:name "Anne"}}})

"Note that this will not work:"

(fact
  (adi/select school-ds
              {:student {:classes
                         {:subject "Science"}}}
              {:pipeline {:fill-assoc
                          {:student
                           {:classes
                            {:subject "Math"}}}}})
  => #{})

"This is because of the generated search query. As can be seen, for the previous case, the query generates a statement where we need to find a subject that has both `Science` and `Math` as names (which is impossible)"

(comment
  (adi/select school-ds
              {:student {:classes
                         {:subject #{"Math" "Science"}}}}
              :raw)
  => [:find ?self :where
      [?self :student/classes ?e160221]
      [?e160221 :class/subject "Science"]
      [?e160221 :class/subject "Math"]])

"Whereas the correct way, is to say that the student takes two different classes, whose names are `Math` for the first and `Science` for the second:"

(comment
  (adi/select school-ds
              {:student {:classes
                         #{{:subject "Science"}
                           {:subject "Math"}}}}
              :raw)
  => [:find ?self :where
      [?self :student/classes ?e160225]
      [?self :student/classes ?e160226]
      [?e160225 :class/subject "Math"]
      [?e160226 :class/subject "Science"]])

[[:section {:title ":ignore"}]]

"Sometimes, there is data that is not in the schema, resulting in an exception being thrown:"

(fact
  (adi/select school-ds
              {:student {:rating 10
                         :classes {:subject "Science"}}})
  => (throws))

"Having `:ignore` params makes sure that the extra entries do not raise any issues"

(fact
  (adi/select school-ds
              {:student {:rating 10
                         :classes
                         #{{:subject "Science"}}}}
              {:pipeline {:ignore
                          {:student {:rating :checked}}}})
  => #{{:student {:name "Anne"}} {:student {:name "Charlie"}}})

[[:section {:title ":allow"}]]

"Allows only entries that have been specified:"

(fact
  (adi/select school-ds
              {:student {:classes
                         #{{:subject "Science"}}}}
              {:pipeline {:allow
                          {:student
                           {:classes {:subject :checked}}}}})
  => #{{:student {:name "Anne"}}
       {:student {:name "Charlie"}}})

"If there is a search on entries that are not allowed, an exception will be thrown:"

(fact
  (adi/select school-ds
              {:student/name "Anne"}
              {:pipeline {:allow
                          {:student
                           {:classes {:subject :checked}}}}})
  => (throws))

"When the map is empty, everything is restricted:"

(fact
  (adi/select school-ds
              {:student {:classes
                         #{{:subject "Science"}}}}
              {:pipeline {:allow {}}})
  => (throws))

[[:section {:title ":validate"}]]

"Validates the map using a function of two parameters:"

(fact
  (adi/select school-ds
              {:student/name "Anne"}
              {:validate {:name "Anne"}
               :pipeline {:validate
                          {:student
                           {:name
                            (fn [x adi]
                              (= x
                                 (-> adi :validate :name)))}}}})
  => #{{:student {:name "Anne"}}})

"If the validation fails, an exception is thrown:"

(fact
  (adi/select school-ds
              {:student/name "Anne"}
              {:pipeline {:validate
                          {:student
                           {:name (fn [x _] (not= "Anne" x))}}}})
  => (throws))

[[:section {:title ":convert"}]]

"Same as the `pre` and `post` transforms, just another place in the pipeline where a generic function can be used:"

(fact
  (adi/select school-ds
              {:student/name "anne"}
              {:pipeline {:convert
                          {:student {:name capitalise}}}})
  => #{{:student {:name "Anne"}}})

"Works with one or two arguments:"

(fact
  (adi/select school-ds
              {:student/name "anne"}
              {:fn capitalise
               :pipeline {:convert
                          {:student
                           {:name (fn [x adi]
                                    ((:fn adi) x))}}}})
  => #{{:student {:name "Anne"}}})

[[:section {:title ":post-require"}]]

"Same as `:pre-require` but happens later in the pipeline:"

(fact
  (adi/select school-ds
              {:student {:classes {:subject "Math"}}}
              {:pipeline {:fill-assoc
                          {:student {:name "Bob"}}
                          :post-require
                          {:student {:name :checked}}}})
  => #{{:student {:name "Bob"}}})

"It can be seen that if the above statement was replaced with `:pre-require`, an exception would be thrown because `:fill-assoc` occurs after `:post-require` but before `:post-require`. Since that particular stage has not been run, the require check shows an empty entry:"

(fact
  (adi/select school-ds
              {:student {:classes {:subject "Math"}}}
              {:pipeline {:fill-assoc
                          {:student {:name "Bob"}}
                          :pre-require
                          {:student {:name :checked}}}})
  => (throws))


[[:section {:title ":post-mask"}]]

"Like `:pre-mask`, but occurs after the validation and fill stages:"

(fact
  (adi/select school-ds
              {:student {:classes {:subject "Math"}}}
              {:pipeline {:fill-assoc {:student {:name "Bob"}}
                          :post-mask
                          {:student {:name (fn [_] true)}}}})
  => #{{:student {:name "Bob"}}
       {:student {:name "Anne"}}})

"Compared to what happens when using `:pre-mask`:"

(fact
  (adi/select school-ds
              {:student {:classes {:subject "Math"}}}
              {:pipeline {:fill-assoc {:student {:name "Bob"}}
                          :pre-mask
                          {:student {:name :checked}}}})
  => #{{:student {:name "Bob"}}})

[[:section {:title ":post-transform"}]]

"Like `:pre-transform` but later in the pipeline:"

(fact
  (adi/select school-ds
              {:student {:classes {:subject "Math"}}}
              {:pipeline {:fill-assoc {:student {:name "BOB"}}
                          :post-transform
                          {:student {:name capitalise}}}})
  => #{{:student {:name "Bob"}}})

"Compared with a similar call to `:pre-transform`:"

(fact
  (adi/select school-ds
              {:student {:classes {:subject "Math"}}}
              {:pipeline {:fill-assoc {:student {:name "BOB"}}
                          :pre-transform
                          {:student {:name capitalise}}}})
  => #{})

[[:section {:title ":post-process"}]]

"Final function that is called on the datastructure at the end of the pipeline:"

(fact
  (adi/select school-ds
              {:student/name "Charlie"}
              {:pipeline
               {:post-process
                (fn [x _]
                  (assoc-in x [:student :name] #{"Bob"}))}})
  => #{{:student {:name "Bob"}}})


