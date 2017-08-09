(ns spirit.core.httpkit.websocket
  (:require [clojure.core.async :as async]
            [org.httpkit.server :as httpkit]))

(def commands
  #{:channel/handshake
    :channel/heartbeat
    :channel/get-id
    :channel/set-id
    :channel/close
    :channel/get-state
    :channel/set-state})

(defn channel-handshake [conn package])

(defn channel-heartbeat [conn package])

(defn channel-get-id [conn package])

(defn channel-get-id [conn package])
