(ns adi.core.link
  (:require [hara.common [checks :refer [hash-map?]]]
            [adi.core.prepare :as prepare]
            [datomic.api :as datomic]))

(declare linked-ids)

(defn wrap-linked-set [f]
  (fn [entrf pmodel rmodel seen fsch]
    (cond (set? entrf)
          (mapv #(f % pmodel rmodel seen fsch)
                (filter #(not (@seen (:db/id %))) entrf))
          :else
          (if (not (@seen (:db/id entrf)))
            (f entrf pmodel rmodel seen fsch)))))

(defn linked-ids-loop
  ([ent rmodel fsch]
     (linked-ids ent rmodel rmodel (atom #{}) fsch))
  ([ent pmodel rmodel seen fsch]
     (if-let [[k v] (first pmodel)]
       (do (cond (hash-map? v)
                 (if-let [entrf (get ent (-> fsch k first :ref :key))]
                   ((wrap-linked-set linked-ids)
                    entrf v rmodel seen fsch))

                 (= v :yield)
                 (if-let [entrf (get ent (-> fsch k first :ref :key))]
                   ((wrap-linked-set linked-ids)
                    entrf rmodel rmodel seen fsch)))
           (recur ent (next pmodel) rmodel seen fsch)))))

(defn linked-ids
  ([ent rmodel fsch]
     (let [seen (atom #{})]
       (linked-ids ent rmodel rmodel seen fsch)
       @seen))
  ([ent pmodel rmodel seen fsch]
     (swap! seen conj (:db/id ent))
     (linked-ids-loop ent pmodel rmodel seen fsch)))

(defn linked-entities [ent rmodel adi]
  (let [adi (prepare/prepare adi {})]
    (map #(datomic/entity (:db adi) %) (linked-ids ent rmodel (-> adi :schema :flat)))))
