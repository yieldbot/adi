(ns documentation.spirit-data-pipeline
  (:use hara.test)
  (:require [spirit.data.pipeline :as pipeline]))

[[:chapter {:title "Introduction"}]]

"[spirit.data.pipeline](https://www.github.com/zcaudate/spirit) provides methods for "

[[:section {:title "Installation"}]]

"
Add to `project.clj` dependencies:

    [im.chit/spirit.data.pipeline \"{{PROJECT.version}}\"]
"

"All functions are in the `spirit.data.pipeline` namespace."

(comment 
  (require '[spirit.data.pipeline :as pipeline]))

[[:section {:title "Motivation"}]]

"
The ideas of `data.atom`
"

[[:chapter {:title "API" :link "spirit.data.pipeline"}]]

[[:api {:title "" :namespace "spirit.data.pipeline"}]]
