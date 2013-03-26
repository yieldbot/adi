(ns adi.api
  (:use adi.utils)
  (:require [datomic.api :as d]
            [adi.data :as ad]
            [adi.schema :as as]))

(defn emit-schema
  "Generates all schemas using a datamap that can be installed
   in the datomic database."
  ([geni]
    (as/build-schema geni))
  ([geni & genis] (emit-schema (apply merge geni genis))))


(defn emit-ref-set [fgeni opts]
  (let [ks    (keys fgeni)
        rks   (filter #(= (-> % fgeni first :type) :ref) ks)
        frks  (if-let [nss (:ns-set opts)]
                (filter (fn [k] (some #(key-ns? k %) nss)) rks)
                rks)]
    (set frks)))

(defn remove-empty-refs [coll]
  (filter (fn [x]
              (or (vector? x)
                  (and (hash-map? x)
                       (-> (dissoc x :db/id) empty? not))))
          coll))

(defn emit-insert
  [data opts]
  (cond (or (vector? data) (list? data) (lazy-seq? data))
        (mapcat #(emit-insert % opts) data)

        (hash-map? data)
        (let [geni   (:geni opts)
              fgeni  (:fgeni opts)
              pdata  (ad/process data geni opts)
              chdata (ad/characterise pdata fgeni (merge {:generate-ids true} opts))]
          (remove-empty-refs (ad/build chdata)))))


(defn emit-update
  [data opts]
  (cond (or (vector? data) (list? data) (lazy-seq? data))
        (mapcat #(emit-insert % opts) data)

        (hash-map? data)
        (let [geni   (:geni opts)
              fgeni  (:fgeni opts)
              pdata  (ad/process data geni (merge {:required? false
                                                   :extras? true
                                                   :defaults? false} opts))
              chdata (ad/characterise pdata fgeni opts)]
          (remove-empty-refs (ad/build chdata)))))

(defn emit-query [data opts]
  (let [geni   (:geni opts)
        fgeni  (:fgeni opts)
        pdata (ad/process data geni (merge {:restrict? false
                                            :required? false
                                            :defaults? false
                                            :sets-only? true} opts))
        chdata (ad/characterise pdata fgeni (merge {:generate-syms true} opts))]
    (ad/clauses chdata opts)))




(defn install-schema [geni conn]
  (d/transact conn (emit-schema geni)))

(defn connect!
  ([uri] (connect! uri false))
  ([uri recreate?]
     (if recreate? (d/delete-database uri))
     (d/create-database uri)
     (d/connect uri)))

(defn insert! [data conn opts]
  (let [cmd (emit-insert data opts)]
    (d/transact conn cmd)))

(defn select-ids [val db opts]
  (cond (number? val) (hash-set val)

        (keyword? val) (select-ids {val '_} db opts)

        (hash-map? val)
        (->> (d/q (emit-query val opts) db)
             (map first)
             set)

        (or (list? val) (vector? val))
        (->> (d/q val db)
             (map first)
             set)

        (or (set? val))
        (set (mapcat #(select-ids % db opts) val))))

(defn select-entities [val db opts]
  (map #(d/entity db %) (select-ids val db opts)))

(defn select-first-entity [val db opts]
  (first (select-entities val db opts)))

(defn select [val db opts]
  (map #(ad/unprocess % opts)
       (select-entities val db opts)))

(defn select-first [val db opts]  (first (select val db opts)))

(defn update! [val data conn opts]
  (let [ids     (select-ids val (d/db conn) opts)
        id-data (map #(assoc data :db/id %) ids)
        cmds (mapcat #(emit-update % opts) id-data)]
    (d/transact conn cmds)))

(declare retract!
         retract-cmd)

(defn retract! [val ks conn opts]
  (let [ents (select-entities val (d/db conn) opts)
        cmds (->> (for [ent ents
                        k ks]
                    (retract-cmd ent k))
                  flatten-to-one
                  (filter identity))]
    (d/transact conn cmds)))

(defn- retract-cmd [ent k]
  (let [id  (:db/id ent)
        [k v] (if (vector? k) k
                  [k (k ent)])]
    (if (and id v)
      (if (set? v)
        (map (fn [x] [:db/retract id k x]) v)
        [:db/retract id k v]))))

(defn all-ref-ids
  ([ent rrs] (set (all-ref-ids ent rrs #{})))
  ([ent rrs exclude]
     (concat [(:db/id ent)]
             (->> rrs
                  (mapcat (fn [k] (let [v (k ent)
                                       id (:db/id v)]
                                   (if (and (ref? v)
                                            (not (exclude id)))
                                     (all-ref-ids v rrs (conj exclude id))))))
                  (filter identity)))))

(defn delete!
  [val conn opts]
  (let [db (d/db conn)
        ids  (if-let [rrs (or (:ref-set opts)
                              (if-let [nss (:ns-set opts)]
                                (emit-ref-set (:fgeni opts) opts))
                              #{})]
               (mapcat #(all-ref-ids % rrs) (select-entities val db opts))
               (select-ids val db opts))
        data (map (fn [x] [:db.fn/retractEntity x]) ids)]
    (d/transact conn data)))
