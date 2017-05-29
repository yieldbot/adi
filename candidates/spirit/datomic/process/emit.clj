(ns spirit.process.emit
  (:require [spirit.process.emit [characterise :as characterise]
                              [datoms :as datoms]
                              [query :as query]]))

(defn emit [spirit]
  (let [nspirit (characterise/characterise spirit)]
    (condp = (:type nspirit)
      "query"  (query/query nspirit)
      "datoms" (datoms/datoms nspirit))))
