(ns adi.process.pack
  (:require [hara.common.error :refer [error]]
            [adi.process.pack [analyse :as analyse]
                              [review :as review]]))

(defn pack [adi]
  (let [data (-> adi :process :normalised)]
    (if (-> adi :options :skip-pack)
      (assoc-in adi [:process :reviewed] data)
      (-> adi
          (analyse/analyse)
          (review/review)))))
