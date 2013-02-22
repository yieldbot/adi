(ns anadi.query
  (:use [anadi.data :only [iid]]
        anadi.utils)
  (:require [datomic.api :as d]))

(defn find-ids [db val]
  (cond (number? val) (find-ids {:db/id val})

        (hash-map?)
        (->> (d/q (concat '[:find ?e :where]
                          (mapv (fn [pair] (cons '?e pair)) val))
                  db)
             (map first))

        (or (vector? val)
            (set? val))
        (mapcat find-ids val)))

(defn find-entities [db val]
  (map #(d/entity db %) (find-ids db val)))
