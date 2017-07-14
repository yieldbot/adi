(ns spirit.transport.websocket
  (:require [spirit.transport.websocket.base :as base]))

(defn create [m]
  (base/create m))
  
;;#?(:cljs (require 'spirit.transport.websocket.default))

