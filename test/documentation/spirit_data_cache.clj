(ns documentation.spirit-data-cache
  (:use hara.test)
  (:require [spirit.data.cache :as cache]))

[[:chapter {:title "Introduction"}]]

"[spirit.data.cache](https://www.github.com/zcaudate/spirit) provides an interface that makes woring with caches easy. Supported implementations are redis and an atom-backed cache used for mocking."

[[:section {:title "Installation"}]]

"
Add to `project.clj` dependencies:

    [im.chit/spirit.data.cache \"{{PROJECT.version}}\"]

For interfacing with redis, please also include the relevant dependencies:
    
    [im.chit/spirit.redis     \"{{PROJECT.version}}\"]    
"

"All functions are in the `spirit.data.cache` namespace."

(comment 
  (require '[spirit.data.cache :as cache]
           '[spirit.redis :as redis]))

[[:section {:title "Motivation"}]]

"
The ideas of `data.cache` has been based around [cassius](https://github.com/MyPost/cassius) and the abstractions that provided simpler reads and writes for cassandra data.
"

[[:chapter {:title "API" :link "spirit.data.cache"}]]

[[:api {:title "" :namespace "spirit.data.cache"}]]

[[:chapter {:title "Walkthrough"}]]

""
