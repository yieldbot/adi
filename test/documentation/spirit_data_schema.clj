(ns documentation.spirit-data-schema
  (:use hara.test)
  (:require [spirit.data.schema :as schema]))

[[:chapter {:title "Introduction"}]]

"[spirit.data.schema](https://www.github.com/zcaudate/spirit) provides methods for outputting data from nested structures"

[[:section {:title "Installation"}]]

"
Add to `project.clj` dependencies:

    [im.chit/spirit.data.schema \"{{PROJECT.version}}\"]
"

"All functions are in the `spirit.data.schema` namespace."

(comment 
  (require '[spirit.data.schema :as schema]))

[[:section {:title "Motivation"}]]

"
The ideas of `data.atom`
"

[[:chapter {:title "API" :link "spirit.data.schema"}]]

[[:api {:title "" :namespace "spirit.data.schema"}]]
