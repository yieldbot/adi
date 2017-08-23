(ns documentation.spirit-data-table
  (:use hara.test)
  (:require [spirit.data.table :as table]))

[[:chapter {:title "Introduction"}]]

"[spirit.data.table](https://www.github.com/zcaudate/spirit) provides methods for outputting data from nested structures"

[[:section {:title "Installation"}]]

"
Add to `project.clj` dependencies:

    [im.chit/spirit.data.table \"{{PROJECT.version}}\"]
"

"All functions are in the `spirit.data.table` namespace."

(comment 
  (require '[spirit.data.table :as table]))

[[:section {:title "Motivation"}]]

"
The ideas of `data.atom`
"

[[:chapter {:title "API" :link "spirit.data.table"}]]

[[:api {:title "" :namespace "spirit.data.table"}]]
