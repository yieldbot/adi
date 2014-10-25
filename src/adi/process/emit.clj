(ns adi.process.emit
  (:require [adi.process.emit [characterise :as characterise]
                              [datoms :as datoms]
                              [query :as query]]))

(defn emit [pdata env]
  (let [chdata (characterise/characterise pdata env)]
    (condp = (:type env)
      "query"  (query/query chdata env)
      "datoms" (datoms/datoms chdata))))
