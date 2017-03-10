(ns adi.process.emit
  (:require [adi.process.emit [characterise :as characterise]
                              [datoms :as datoms]
                              [query :as query]]))

(defn emit [adi]
  (let [nadi (characterise/characterise adi)]
    (condp = (:type nadi)
      "query"  (query/query nadi)
      "datoms" (datoms/datoms nadi))))
