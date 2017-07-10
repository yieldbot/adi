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
  (merge transport/*default-config*))

(defn request
  ([client package]
   (request client package {}))
  ([client package opts]
   (transport/-request client package opts)))

;;#?(:cljs (require 'spirit.common.http.client.default))

