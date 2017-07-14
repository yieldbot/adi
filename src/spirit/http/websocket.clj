(ns spirit.http.websocket
  (:require [spirit.http.websocket.base :as base]))

(defn create [m]
  (base/create m))
  
;;#?(:cljs (require 'spirit.http.websocket.default))

