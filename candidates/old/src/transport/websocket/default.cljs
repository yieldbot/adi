(ns spirit.common.http.websocket.default
  (:require [spirit.common.http.websocket.base :as base])

(defmethod base/create :default
  [{:keys [url] :as m}]
  (let [socket (js/WebSocket. url)]
    socket))