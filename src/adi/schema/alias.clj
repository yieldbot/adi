(ns adi.schema.alias
  (:require [hara.data.path :as data]))

(defn prepare-aliases [fschm]
  (let [aliases (->> fschm
                     (filter (fn [[k [attr]]]
                               (= :alias (:type attr))))
                     (map second))]
    (map (fn [[attr]]
           [(assoc-in attr [:alias :template]
                      (data/treeify-keys-nested (-> attr :alias :template)))])
         aliases)))
