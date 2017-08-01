(ns spirit.core.datomic.process.emit
  (:require [spirit.core.datomic.process.emit
             [characterise :as characterise]
             [datoms :as datoms]
             [query :as query]]))

(defn emit [datasource]
  (let [ndatasource (characterise/characterise datasource)]
    (condp = (:command ndatasource)
      :query  (query/query ndatasource)
      :datoms (datoms/datoms ndatasource))))
