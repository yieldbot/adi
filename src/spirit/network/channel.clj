(ns spirit.network.channel
  (:require [clojure.core.async :as async]))

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
