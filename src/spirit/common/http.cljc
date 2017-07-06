(ns spirit.common.http
  (:require [spirit.http.common :as common]))

(defn push
  ([conn data]
   (push conn data {}))
  ([conn data opts]
   (common/push conn data opts)))

(defn request
  ([conn data]
   (push conn data {}))
  ([conn data opts]
   (common/push conn data opts)))
