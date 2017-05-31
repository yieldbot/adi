(ns spirit.datomic.process.pack
  (:require [hara.common.error :refer [error]]
            [spirit.datomic.process.pack [analyse :as analyse]
                              [review :as review]]))

(defn pack [datasource]
  (let [data (-> datasource :process :normalised)]
    (if (-> datasource :options :skip-pack)
      (assoc-in datasource [:process :reviewed] data)
      (-> datasource
          (analyse/analyse)
          (review/review)))))
