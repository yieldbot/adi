(ns documentation.adi-guide.options.normalise
  (:use midje.sweet)
  (:require [adi.core :as adi]
            [hara.namespace.import :as ns]
            [documentation.adi-guide.options.query]))

(ns/import documentation.adi-guide.options.query [school-ds])


[[:section {:title ":schema-required"}]]

(adi/select school-ds {:student/classes/subject "Math"})
=> #{{:student {:name "Bob"}} {:student {:name "Anne"}}}

(adi/select school-ds {:student/classes/subject "Math"}
            :schema-required)

[[:section {:title ":schema-restrict"}]]
[[:section {:title ":schema-defaults"}]]
