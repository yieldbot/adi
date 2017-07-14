(ns spirit.http
  (:require [spirit.http.client :as client]
            [spirit.http.server :as server]
            [spirit.http.transport :as transport]
            [spirit.http.websocket :as websocket]))

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
