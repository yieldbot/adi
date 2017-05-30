(ns spirit.datomic.process.emit
  (:require [spirit.datomic.process.emit
             [characterise :as characterise]
             [datoms :as datoms]
             [query :as query]]))

(defn emit [datasource]
  (let [ndatasource (characterise/characterise datasource)]
    (condp = (:type ndatasource)
      "query"  (query/query ndatasource)
      "datoms" (datoms/datoms ndatasource))))
