(ns spirit.common.http.client
  (:require [spirit.common.http.client.base :as base]))

(defn create [m]
  (base/create m))
  
#?(:cljs (require 'spirit.common.http.client.default))
