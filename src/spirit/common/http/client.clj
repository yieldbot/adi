(ns spirit.common.http.client
  (:require [spirit.common.http.client.base :as base]
            [spirit.common.http.transport :as transport]))

(defn create [m]
  (base/create m))

(def ^:dynamic *default-wrapper-list*
  [:wrap-parse-data
   :wrap-routes
   :wrap-path-uri
   :wrap-trim-uri
   :wrap-transport])

(def ^:dynamic *default-config*
  (merge transport/*default-config*
         {:return :value}))

(defn request
  ([client package]
   (transport/-request client package))
  ([client package opts]
   (request (merge client opts) package)))

;;#?(:cljs (require 'spirit.common.http.client.default))

