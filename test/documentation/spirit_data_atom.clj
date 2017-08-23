(ns documentation.spirit-data-cache
  (:use hara.test)
  (:require [spirit.data.cache :as cache]))

[[:chapter {:title "Introduction"}]]

"[spirit.data.atom](https://www.github.com/zcaudate/spirit) provides methods for working with atoms"

[[:section {:title "Installation"}]]

"
Add to `project.clj` dependencies:

    [im.chit/spirit.data.atom \"{{PROJECT.version}}\"]
"

"All functions are in the `spirit.data.atom` namespace."

(comment 
  (require '[spirit.data.atom :as atom]))

[[:section {:title "Motivation"}]]

"
The ideas of `data.atom`
"

[[:chapter {:title "API" :link "spirit.data.atom" :exclude ["attach-state" "detach-state" "read-value" "write-value"]}]]

[[:api {:title "" :namespace "spirit.data.atom" :exclude ["attach-state" "detach-state" "read-value" "write-value"]}]]
