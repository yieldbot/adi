(ns adi.api
  (:use [adi.data :only [iid]]
        adi.utils)
  (:require [datomic.api :as d]
            [adi.data :as ad]
            [adi.schema :as as]))

(defn q-select [db fsm rset query & args]
  (let [res  (apply d/q query db args)
        ids  (map first res)
        ents (map #(d/entity db %) ids)]
    (map #(ad/unprocess fsm % rset) ents)))

(declare select-ids
         select-ids-query make-clauses append-clauses)

(defn select-ids [db val]
  (cond (number? val) (hash-set val)

        (keyword? val) (select-ids db {val '_})

        (or (hash-map? val) (vector? val) (list? val))
        (->> (d/q (select-ids-query val) db)
             (map first)
             set)

        (or (set? val))
        (set (mapcat #(select-ids db %) val))))

(defn select-ids-query [arr]
  (let [dsym     (gen-dsym)
        clauses  (make-clauses dsym arr)]
    (concat [:find dsym :where] clauses)))

(defn not-clause [dsym k v output]
  (let [nsym (gen-dsym)]
    (conj output [dsym k nsym] [(list 'not= v nsym)])))

(defn append-not-clauses [dsym arr output]
  (if-let [[k v] (first arr)]
    (append-not-clauses dsym (next arr)
                        (not-clause dsym k v output))
    output))

(defn fulltext-clause [dsym k v output]
  (let [fsym (name (gen-dsym))]
    (conj output [(list 'fulltext '$ k v) [[dsym fsym]]])))

(defn append-fulltext-clauses [dsym arr output]
  (if-let [[k v] (first arr)]
    (append-fulltext-clauses dsym (next arr)
                             (fulltext-clause dsym k v output))
    output))

(defn not-all-clause [dsym k v output]
  (let [fsym (name (gen-dsym))]
    (conj output [(list 'fulltext '$ k v) [[dsym fsym]]])))

(defn append-not-all-clauses [dsym arr output]
  (if-let [[k v] (first arr)]
    (append-not-all-clauses dsym (next arr)
                             (fulltext-clause dsym k v output))
    output))


(defn append-clauses [dsym k v output]
  (cond (not= :+ (key-ns k))
        (conj output [dsym k v])

        (= :+/not k)
        (append-not-clauses dsym v output)

        (= :+/fulltext k)
        (append-fulltext-clauses dsym v output)

        :else (throw (Exception. (str "Cannot interpret key: " k)))))

(defn make-clauses
    ([dsym arr] (make-clauses dsym arr []))
    ([dsym arr output]
       (if-let [[k v] (first arr)]
         (make-clauses dsym (next arr) (append-clauses dsym k v output))
         output)))



(comment
  (defn select-ids-query [arr]
    (let [dsym     (name (gen-dsym))
          clauses  (make-clauses dsym arr)]
      (format "[:find %s :where %s]" dsym clauses)))

  (defn to-string [v]
    (cond (string? v) (str \" v \")
          (keyword v) (format "(keyword \"%s\")" (key-str v))
          :else v))

  (defn not-clause [dsym k v]
    (let [nsym (name (gen-dsym))]
      (format "[%s %s %s] [(not= %s %s)]"
              dsym k nsym (to-string v) nsym)))

  (defn append-not-clauses [dsym arr output]
    (if-let [[k v] (first arr)]
      (append-not-clauses dsym (next arr)
                          (str output " " (not-clause dsym k v)))
      output))

  (defn fulltext-clause [dsym k v]
    (let [fsym (name (gen-dsym))]
      (format "[(fulltext $ %s %s) [[%s %s]]]" k (to-string v) dsym fsym)))

  (defn append-fulltext-clauses [dsym arr output]
    (if-let [[k v] (first arr)]
      (append-fulltext-clauses dsym (next arr)
                               (str output " " (fulltext-clause dsym k v)))
      output))

  (defn append-clauses [dsym k v output]
    (cond (not= :+ (key-ns k))
          (str output " " (format "[%s %s %s]" dsym k (to-string v)))

          (= :+/not k)
          (append-not-clauses dsym v output)

          (= :+/fulltext k)
          (append-fulltext-clauses dsym v output)

          :else (throw (Exception. (str "Cannot interpret key: " k)))))


  (defn make-clauses
    ([dsym arr] (make-clauses dsym arr ""))
    ([dsym arr output]
       (if-let [[k v] (first arr)]
         (make-clauses dsym (next arr) (append-clauses dsym k v output))
         output))))


(defn select-entities [db val]
  (map #(d/entity db %) (select-ids db val)))

(defn select-first-entity [db val]
  (first (select-entities db val)))

(defn select [db fsm val rset]
  (map #(ad/unprocess fsm % rset)
       (select-entities db val)))

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

(defn insert! [conn fsm data]
  (let [cmd (ad/emit fsm data)]
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
