(ns adi.api
  (:use adi.utils
        [adi.schema :only [emit-schema]]
        [adi.data :only [emit-datoms
                         emit-datoms-insert
                         emit-datoms-update
                         emit-query]])
  (:require [datomic.api :as d]
            [adi.data :as ad]
            [adi.schema :as as]))

(defn install-schema [fgeni conn]
  (d/transact conn (emit-schema fgeni)))

(defn connect!
  ([uri] (connect! uri false))
  ([uri recreate?]
     (if recreate? (d/delete-database uri))
     (d/create-database uri)
     (d/connect uri)))

(defn insert- [data env]
  (emit-datoms-insert data env))

(defn insert! [data conn env]
  (d/transact conn (insert- data env)))

(defn select-ids [val db env]
  (cond (number? val) (hash-set val)

        (keyword? val) (select-ids {val '_} db env)

        (hash-map? val)
        (->> (d/q (emit-query val env) db)
             (map first)
             set)

        (or (list? val) (vector? val))
        (->> (d/q val db)
             (map first)
             set)

        (or (set? val))
        (set (mapcat #(select-ids % db env) val))))

(defn select-entities [val db env]
  (map #(d/entity db %) (select-ids val db env)))

(defn select-first-entity [val db env]
  (first (select-entities val db env)))

(defn update- [val data db env]
  (let [ids     (select-ids val db env)
        id-data (map #(assoc data :db/id %) ids)]
    (mapcat #(emit-datoms-update % env) id-data)))

(defn update! [val data conn env]
  (d/transact conn (update- val data (d/db conn) env)))

(defn- retract-cmd [ent k]
  (let [id  (:db/id ent)
        [k v] (if (vector? k) k
                  [k (k ent)])]
    (if (and id v)
      (if (set? v)
        (map (fn [x] [:db/retract id k x]) v)
        [:db/retract id k v]))))

(defn retract- [val ks db env]
  (let [ents (select-entities val db env)]
    (->> (for [ent ents
               k ks]
           (retract-cmd ent k))
         flatten-to-vecs
         (filter identity))))

(defn retract! [val ks conn env]
  (d/transact conn (retract- val ks (d/db conn) env)))

(defn delete-
  [val db env]
  (let [ids  (select-ids val db env)]
    (map (fn [x] [:db.fn/retractEntity x]) ids)))

(defn delete!
  [val conn env]
  (d/transact conn (delete- val (d/db conn env))))

(defn select [val db env]
  (let [menv (if (-> env :deprocess)
               env (assoc env :deprocess
                          {:data-default :show
                           :refs-default :ids}))]
    (map #(ad/deprocess % menv)
         (select-entities val db env))))

(defn select-first [val db env]  (first (select val db env)))

(declare all-linked-ids)

(defn find-linked-nss [fgeni view]
  (set (filter (fn [k] (= :ref (-> fgeni k :type)))
               (keys (flatten-keys-in view)))))

(defn all-linked-ids-key
  [k ent vnss env exclude]
  (let [v (k ent)
        id (:db/id v)]
    (if (and (ref? v)
             (not (exclude id)))
      (all-linked-ids v vnss env (conj exclude id)))))

(defn all-linked-ids
  ([ent view env]
     (let [vnss (find-linked-nss (-> env :schema :fgeni)
                                 view)]
       (set (all-linked-ids ent vnss env #{}))))
  ([ent vnss env exclude]
     (concat [(:db/id ent)]
             (->> vnss
                  (mapcat #(all-linked-ids-key % ent vnss env exclude))
                  (filter identity)))))
