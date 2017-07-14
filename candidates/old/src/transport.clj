(ns spirit.transport
  (:require [spirit.transport.client :as client]
            [spirit.transport.server :as server]
            [spirit.transport.net :as transport]
            [spirit.transport.websocket :as websocket]))

(defn server [m]
  (server/create m))

(defn client [m]
  (client/create m))

(defn ws-client [m]
  (websocket/create m))

(defn push
  ([conn data]
   (push conn data {}))
  ([conn data opts]
   (transport/-push conn data opts)))

(defn request
  ([conn data]
   (push conn data {}))
  ([conn data opts]
   (transport/-request conn data opts)))
