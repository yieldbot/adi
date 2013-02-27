(ns adi.history
  (:use adi.utils)
  (:require [adi.schema :as as]
            [adi.data :as ad]
            [datomic.api :as d]))


(defn attr-tx [db val attr]
  (let [query '[:find ?t ?v :in $ ?e ?a
                :where
                [?e ?a ?v ?t]]
        id    (ad/get-id val)]
    (if id (first (d/q query db id attr)))))


(defn attr-tx-prev [db val attr]
  (if-let [[t v] (attr-tx db val attr)]
    (let  [pdb (d/as-of db (dec t))
           [pt pv] (attr-tx pdb val attr)]
      (if (not= pt t) [pt pv]))))


(defn attr-tx-all
  ([db val attr]
     (if-let [[t v] (attr-tx db val attr)]
       (attr-tx-all db val attr [[t v]]) []))
  ([db val attr output]
     (if-let [prev (attr-tx-prev db val attr)]
       (attr-tx-all db val attr (conj output prev))
       output)))

(defn attr-tx-first [db val attr]
  (last (attr-tx-all db val attr)))

(defn tx [db val]
  (let [query '[:find ?t :in $ ?e
                :where
                [?e _ _ ?t _]]
        id    (ad/get-id val)]
    (if id
      (->> (d/q query db id)
          (map first)
          ;;(apply max)
          ))))

(defn tx-prev [db val]
  (if-let [[t v] (tx db val)]
    (let  [pdb (d/as-of db t)
           [pt pv] (tx pdb val)]
      (if (not= pt t) [pt pv]))))
