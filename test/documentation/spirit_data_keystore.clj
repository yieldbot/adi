(ns documentation.spirit-data-keystore
  (:use hara.test)
  (:require [spirit.data.keystore :as keystore]))

[[:chapter {:title "Introduction"}]]

"[spirit.data.keystore](https://www.github.com/zcaudate/spirit) provides an interface that makes working with keystores and keystore-like interfaces easy. Currently, the interface supports cassandra, dynamodb, redis and an atom-backed store used for mocking."

[[:section {:title "Installation"}]]

"
Add to `project.clj` dependencies:

    [im.chit/spirit.data.keystore \"{{PROJECT.version}}\"]

For interfacing with cassandra, dynamodb and redis, please also include the relevant dependencies:
    
    [im.chit/spirit.cassandra \"{{PROJECT.version}}\"]
    [im.chit/spirit.dynamodb  \"{{PROJECT.version}}\"]
    [im.chit/spirit.redis     \"{{PROJECT.version}}\"]    
"

"All functions are in the `spirit.data.keystore` namespace."

(comment 
  (require '[spirit.data.keystore :as ks]
           '[spirit.redis :as redis]))

[[:section {:title "Motivation"}]]

"
The ideas of `data.keystore` has been based around [cassius](https://github.com/MyPost/cassius) and the abstractions that provided simpler reads and writes for cassandra data.
"

[[:chapter {:title "API" :link "spirit.data.keystore"}]]

[[:api {:title "" :namespace "spirit.data.keystore"}]]

[[:chapter {:title "Walkthrough"}]]

""
