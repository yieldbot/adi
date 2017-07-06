(ns spirit.common.http.client.default
  (:require [spirit.common.http.client.base :as base])

(defmethod base/create :default
  [{:keys [url] :as m}]
  (let [socket (js/WebSocket. url)]
    socket))