(ns spirit.common.http.websocket
  (:require [spirit.common.http.websocket.base :as base]))

(defn create [m]
  (base/create m))
  
#?(:cljs (require 'spirit.common.http.websocket.default))
