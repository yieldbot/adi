(ns documentation.spirit-data-exchange
  (:use hara.test)
  (:require [spirit.data.exchange :as exchange]))

[[:chapter {:title "Introduction"}]]

"[spirit.data.exchange](https://www.github.com/zcaudate/spirit) provides an interface that makes working with exchanges and exchange-like interfaces easy. Currently, the interface supports cassandra, dynamodb, redis and an atom-backed store used for mocking."

[[:section {:title "Installation"}]]

"
Add to `project.clj` dependencies:

    [im.chit/spirit.data.exchange \"{{PROJECT.version}}\"]

For interfacing with cassandra, dynamodb and redis, please also include the relevant dependencies:
    
    [im.chit/spirit.rabbitmq  \"{{PROJECT.version}}\"]    
"

"All functions are in the `spirit.data.exchange` namespace."

(comment 
  (require '[spirit.data.exchange :as exchange]
           '[spirit.rabbitmq :as rabbitmq]))

[[:section {:title "Motivation"}]]

"
The ideas of `data.exchange` has been based
"

[[:chapter {:title "API" :link "spirit.data.exchange"}]]

[[:api {:title "" :namespace "spirit.data.exchange"}]]

[[:chapter {:title "Walkthrough"}]]
