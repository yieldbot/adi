(ns spirit.jetty.websocket
  (:require [spirit.common.http.websocket.base :as base])

(defrecord JettyWebsocket)

(defmethod base/create :default
  [{:keys [url] :as m}])