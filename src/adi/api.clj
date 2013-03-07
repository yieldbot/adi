(ns adi.api
  (:use adi.utils)
  (:require [datomic.api :as d]
            [adi.emit :as ae]
            [adi.data :as ad]))

(defn install-schema [conn schm]
  (d/transact conn (ae/emit-schema schm)))

(defn connect!
  ([uri] (connect! uri false))
  ([uri recreate?]
     (if recreate? (d/delete-database uri))
     (d/create-database uri)
     (d/connect uri)))

(defn select-ids [db fschm val]
  (cond (number? val) (hash-set val)

        (keyword? val) (select-ids db {val '_})

        (hash-map? val)
        (->> (d/q (ae/emit-query val fschm) db)
             (map first)
             set)

        (or (list? val) (vector? val))
        (->> (d/q val db)
             (map first)
             set)

        (or (set? val))
        (set (mapcat #(select-ids db %) val))))

(defn select-entities [db fschm val]
  (map #(d/entity db %) (select-ids db fschm val)))

(defn select-first-entity [db fschm val]
  (first (select-entities db fschm val)))

(defn select [db fschm val & [rrs]]
  (map #(ad/unprocess fschm % rrs)
       (select-entities db fschm val)))

(defn select-first [db fschm val & [rrs]]
  (first (select db fschm val rrs)))

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

(defn insert! [conn fschm data]
  (let [cmd (ae/emit-insert data fschm)]
    (d/transact conn cmd)))

(defn update! [conn fschm val data]
  (let [ids     (select-ids (d/db conn) val)
        id-data (map #(assoc-in data [:+ :db/id] %) ids)
        cmds (mapcat #(ae/emit-update % fschm) id-data)]
    (d/transact conn cmds)))

(declare retract!
         retract-cmd)

(defn retract! [conn val ks]
  (let [ents (select-entities (d/db conn) val)
        cmds (->> (for [ent ents
                        k ks]
                    (retract-cmd ent k))
                  (filter identity))]
    (d/transact conn cmds)))

(defn- retract-cmd [ent k]
  (let [id  (:db/id ent)
        val (k ent)]
    (if (and id val) [:db/retract id k val])))

(defn delete!
  [conn val & [rset]]
  (let [db (d/db conn)
        ids  (if rset (mapcat #(all-ref-ids % rset) (select-entities db val))
                      (select-ids db val))
        data (map (fn [x] [:db.fn/retractEntity x]) ids)]
    (d/transact conn data)))

;; Database Specific

(defn db-doc
  [db attr]
  (if-let [res (first (d/q '[:find ?e ?doc :in $ ?attr :where
                             [?e :db/ident ?attr]
                             [?e :db/doc ?doc]]
                           db attr))]
    (cons attr res)))

(defn db-txs [db]
  (->> (d/q '[:find ?e ?t :where
              [?e :db/txInstant ?t]]
            db)
       (sort #(compare (second %1) (second %2)))))

(declare db-attrs
         rule-db-attrs)

(defn db-attrs
  ([db & [ns]]
     (->> (d/q '[:find ?e ?ident :in $ % ?ns :where
                (dbAttrs ?e ?ident ?ns)]
               db
               [rule-db-attrs]
               (if ns (str ns "/") ""))
          (sort #(compare (first %1) (first %2))))))

(def rule-db-attrs
  '[[dbAttrs ?e ?ident ?ns]
    [?e :db/ident ?ident]
    ;;[_ :db.install/attribute ?e]
    [(.toString ?ident) ?val]
    [(.startsWith ?val ?ns)]])
