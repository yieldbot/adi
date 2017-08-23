(ns documentation.spirit-data-graph
  (:use hara.test)
  (:require [spirit.data.graph :as graph]))

[[:chapter {:title "Introduction"}]]

"[spirit.data.graph](https://www.github.com/zcaudate/spirit) provides methods for outputting data from nested structures"

[[:section {:title "Installation"}]]

"
Add to `project.clj` dependencies:

    [im.chit/spirit.data.graph \"{{PROJECT.version}}\"]
"

"All functions are in the `spirit.data.graph` namespace."

(comment 
  (require '[spirit.data.graph :as graph]))

[[:section {:title "Motivation"}]]

"
The ideas of `data.atom`
"

[[:chapter {:title "API" :link "spirit.data.graph"}]]

[[:api {:title "" :namespace "spirit.data.graph"}]]
