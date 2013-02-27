(ns adi.api
  (:use [adi.data :only [iid]]
        adi.utils)
  (:require [datomic.api :as d]
            [adi.data :as ad]
            [adi.schema :as as]))

(defn make-clause [pair]
  (let [[k v] pair]
    (if (not= :+ (key-ns k)) ['?e k v])))

(defn id-query [arr]
  (let [clauses (->> arr
                     (map make-clause)
                     (filter identity)
                     vec)]
    (concat '[:find ?e :where] clauses)))

(defn select-ids [db val]
  (cond (number? val) [val]

        (or (hash-map? val) (vector? val) (list? val))
        (->> (d/q (id-query val) db)
             (map first))

        (or (set? val))
        (set (mapcat select-ids val))))

(defn select-entities [db val]
  (map #(d/entity db %) (select-ids db val)))

(defn select-first-entity [db val]
  (first (select-entities db val)))

(defn select [db fsm val rset]
  (map #(ad/unprocess fsm % rset)
       (select-entities db val)))

(defn selectq [db fsm rset query & args]
  (let [res  (apply d/q query db args)
        ids  (map first res)
        ents (map #(d/entity db %) ids)]
    (map #(ad/unprocess fsm % rset) ents)))

(defn all-ref-ids
  ([ent rset] (set (all-ref-ids ent rset #{})))
  ([ent rset exclude]
     (concat [(:db/id ent)]
             (->> rset
                  (mapcat (fn [k] (let [v (k ent)
                                       id (:db/id v)]
                                   (if (and (ref? v)
                                            (not (exclude id)))
                                     (all-ref-ids v rset (conj exclude id))))))
                  (filter identity)))))

(defn delete!
  [conn val & [rset]]
  (let [db (d/db conn)
        ids  (if rset (mapcat #(all-ref-ids % rset) (select-entities db val))
                      (select-ids db val))
        data (map (fn [x] [:db.fn/retractEntity x]) ids)]
    (d/transact conn data)))

(defn insert! [conn fsm data & more]
  (let [cmd (apply ad/emit fsm data more)]
    (d/transact conn cmd)))

(defn update! [conn fsm val data]
  (let [ids     (select-ids (d/db conn) val)
        id-data (map #(assoc-in data [:+ :db/id] %) ids)
        cmds (mapcat #(ad/emit-no-defaults fsm %) id-data)]
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


(def rule-db-attrs
  '[[dbAttrs ?e ?ident ?ns]
    [?e :db/ident ?ident]
    ;;[_ :db.install/attribute ?e]
    [(.toString ?ident) ?val]
    [(.startsWith ?val ?ns)]])

;; Database Specific

(defn db-attrs
  ([db & [ns]]
     (->> (d/q '[:find ?e ?ident :in $ % ?ns :where
                (dbAttrs ?e ?ident ?ns)]
               db
               [rule-db-attrs]
               (if ns (str ns "/") ""))
          (sort #(compare (first %1) (first %2))))))

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




(defn unique-seq
  ([coll] (unique-seq coll identity))
  ([coll f] (unique-seq coll f [] last))
  ([coll f output last]
     (if-let [v (first coll)]
       (cond (and last (= (f last) (f v)))
             (unique-seq (next coll) f output last)
             :else (unique-seq (next coll) f (conj output v) v))
       output)))

(unique-seq [1 1 3 4 5 5 6 7])
