(ns adi.process.pack
  (:require [adi.process.pack [analyse :as analyse]
                              [review :as review]]))

(defn pack [ndata env]
  (if (-> env :options :skip-pack) ndata
    (let [pdata (-> ndata
                    (analyse/analyse env)
                    (review/review env))]
      pdata)))
