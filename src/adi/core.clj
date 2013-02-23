(ns adi.core
  (:require [adi.data :as ad]
            [adi.schema :as as]
            [adi.query :as aq]
            [datomic.api :as d]))

(defn connect-ds [dm uri]
  {:conn (d/connect uri)
   :fm   (flatten-keys dm)})

(defn transact [ds data]
  (d/transact (:conn ds) data))
