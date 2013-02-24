(ns adi.core
  (:use adi.utils)
  (:require [adi.data :as ad]
            [adi.schema :as as]
            [adi.api :as aa]
            [datomic.api :as d]))

(defn connect-ds [dm uri]
  {:conn (d/connect uri)
   :fm   (flatten-keys dm)})

(defn transact [ds data]
  (d/transact (:conn ds) data))

(defn query
  [ds qu & args]
  (apply d/q qu (d/db (:conn ds)) args))
