(ns anadi.core
  (:require [anadi.data :as ad]
            [anadi.schema :as as]
            [anadi.query :as aq]
            [datomic.api :as d]))

(defn connect-ds [dm uri]
  {:conn (d/connect uri)
   :fm   (flatten-keys dm)})

(defn transact [ds data]
  (d/transact (:conn ds) data))
