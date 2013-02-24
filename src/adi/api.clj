(ns adi.api
  (:use [adi.data :only [iid]]
        adi.utils)
  (:require [datomic.api :as d]
            [adi.data :as ad]
            [adi.schema :as as]))

(defn find-ids [db val]
  (cond (number? val) (find-ids {:db/id val})

        (hash-map? val)
        (->> (d/q (concat '[:find ?e :where]
                          (mapv (fn [pair] (cons '?e pair)) val))
                  db)
             (map first))

        (or (list? val)
            (vector? val)
            (set? val))
        (mapcat find-ids val)))

(defn find [db val]
  (map #(d/entity db %) (find-ids db val)))

(defn find-first [db val]
  (first (find db val)))

(defn all-ref-ids
  ([ent rset] (set (all-ref-ids ent rset #{})))
  ([ent rset exclude]
     (concat [(:db/id ent)]
             (->> rset
                  (mapcat (fn [k] (let [v (k ent)
                                       id (:id/id v)]
                                   (if (and (ref? v)
                                            (not (exclude id)))
                                     (all-ref-ids v rset (conj exclude id))))))
                  (filter identity)))))

(defn delete!
  [conn val]
  (let [data (map (fn [x] [:db.fn/retractEntity x])
                  (find-ids (d/db conn) val))]
    (d/transact conn data)))

(defn delete-linked!
  ([conn fm val]
     (delete-linked! conn fm val (as/make-rset fm)))
  ([conn fm val rset]
     (let [db   (d/db conn)
           ids  (mapcat #(all-ref-ids % rset) (find db val))
           data (map (fn [x] [:db.fn/retractEntity x]) ids)]
       (clojure.pprint/pprint data)
       ;;(d/transact conn data)
       )))


(defn add! )

(defn update! [conn fm val tms])

(defn retract! [conn val fm])
