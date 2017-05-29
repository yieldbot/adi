(ns spirit.process.pack
  (:require [hara.common.error :refer [error]]
            [spirit.process.pack [analyse :as analyse]
                              [review :as review]]))

(defn pack [spirit]
  (let [data (-> spirit :process :normalised)]
    (if (-> spirit :options :skip-pack)
      (assoc-in spirit [:process :reviewed] data)
      (-> spirit
          (analyse/analyse)
          (review/review)))))
