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

(defn handshake [conn package])

(defn heartbeat [conn package])

(defn get-id [conn package])

(defn set-id [conn package])

(defn close [conn package])
