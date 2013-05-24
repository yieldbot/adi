(ns adi.api
  (:use hara.common
        adi.utils
        [adi.schema :only [emit-schema]]
        [adi.emit.datoms :only [emit-datoms
                                emit-datoms-insert
                                emit-datoms-update]]
        [adi.emit.query :only [emit-query]])
  (:require [datomic.api :as d]
            [adi.emit.deprocess :as ad]
            [adi.emit.view :as av]
            [adi.schema :as as]
            [adi.api.schema :as aas]))

(defn install-schema [conn fgeni]
  (d/transact conn (emit-schema fgeni)))

(defn connect!
  ([uri] (connect! uri false))
  ([uri recreate?]
     (if recreate? (d/delete-database uri))
     (d/create-database uri)
     (d/connect uri)))

(defn insert- [data env]
  (emit-datoms-insert data env))

(defn insert! [conn data env]
  (d/transact conn (insert- data env)))

(defn- select-patch-keyword [val env]
  (if (aas/schema-property? env val) val
      (or (first (aas/schema-required-keys env val))
          (error val "is not a correct schema property"))))

(defn select-ids [db val env]
  (cond (number? val) (hash-set val)

        (keyword? val)
        (select-ids db {(select-patch-keyword val env) '_} env)

        (hash-map? val)
        (->> (d/q (emit-query val env) db)
             (map first)
             set)

        (or (list? val) (vector? val))
        (->> (d/q val db)
             (map first)
             set)

        (or (set? val))
        (set (mapcat #(select-ids db % env) val))))

(defn select-entities [db val env]
  (map #(d/entity db %) (select-ids db val env)))

(defn select-fields [db val fields env]
  (map #(select-keys % fields) (select-entities db val env)))

(defn select-first-entity [db val env]
  (first (select-entities db val env)))

(defn update- [db val data env]
  (let [ids     (select-ids db val env)
        id-data (map #(assoc data :db/id %) ids)]
    (mapcat #(emit-datoms-update % env) id-data)))

(defn update! [conn val data env]
  (d/transact conn (update- (d/db conn) val data env)))

(defn- retract-cmd [ent k]
  (let [id  (:db/id ent)
        [k v] (if (vector? k) k
                  [k (k ent)])]
    (if (and id v)
      (if (set? v)
        (map (fn [x] [:db/retract id k x]) v)
        [:db/retract id k v]))))

(defn retract- [db val ks env]
  (let [ents (select-entities db val env)]
    (->> (for [ent ents
               k ks]
           (retract-cmd ent k))
         flatten-to-vecs
         (filter identity))))

(defn retract! [conn val ks env]
  (d/transact conn (retract- (d/db conn) val ks env)))

(defn delete-
  [db val env]
  (let [ids  (select-ids db val env)]
    (map (fn [x] [:db.fn/retractEntity x]) ids)))

(defn delete!
  [conn val env]
  (d/transact conn (delete- (d/db conn) val env)))

(defn select [db val env]
  (let [menv (if (-> env :deprocess)
               env (assoc env :deprocess
                          {:data-default :show
                           :refs-default :ids}))]
    (map #(ad/deprocess % menv)
         (select-entities db val env))))

(defn select-first [db val env]  (first (select db val env)))

(declare linked-ids)

(defn linked-nss [fgeni view]
  (set (filter (fn [k] (= :ref (-> fgeni k first :type)))
               (av/view-make-set view))))

(defn linked-ids-ref
  [rf vnss env exclude]
  (let [id (:db/id rf)]
    (if (not (exclude id))
      (linked-ids rf vnss env (conj exclude id))
      [])))

(defn linked-ids-key
  [k ent vnss env exclude]
  (let [[meta] (-> env :schema :fgeni k)
        res ((-> meta :ref :key) ent)]
    (cond (ref? res)
          (linked-ids-ref res vnss env exclude)
          (vector? res)
          (mapcat #(linked-ids-ref % vnss env exclude) res))))

(defn linked-ids
  ([ent env]
     (let [vw (or (-> env :view)
                  (av/view-cfg (-> env :schema :fgeni)
                               {:refs :show}))]
       (linked-ids ent vw env)))
  ([ent view env]
     (let [vnss (linked-nss (-> env :schema :fgeni) view)]
       (set (linked-ids ent vnss env #{}))))
  ([ent vnss env exclude]
     (concat [(:db/id ent)]
             (->> vnss
                  (mapcat #(linked-ids-key % ent vnss env exclude))
                  (filter identity)))))

(defn linked-all-ids [db val env]
  (let [ents (select-entities db val env)]
      (->> ents (mapcat #(linked-ids % env)) set)))

(defn linked-entities
  [db val env]
    (select-entities db (linked-all-ids db val env) env))

(defn linked [db val env]
  [val db env]
    (select db (linked-all-ids db val env) (dissoc env :view :deprocess)))
