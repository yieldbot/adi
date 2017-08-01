(ns spirit.core.datomic.process.emit.datoms
  (:require [spirit.core.datomic.data.checks :refer [db-id?]]
            [hara.common.error :refer [error]]))

(def ^:dynamic *nested* nil)

(defn datom-funcs [chd]
  (let [cid (get-in chd [:# :id])
        dbs (for [[k v] (:db-funcs chd)]
              (apply vector (keyword (first v)) (get-in chd [:# :id]) k (rest v)))]
    (concat
     (mapcat datom-funcs (vals (:refs-one chd)))
     (mapcat datom-funcs (vals (:revs-one chd)))
     (mapcat datom-funcs (apply concat (vals (:refs-many chd))))
     (mapcat datom-funcs (apply concat (vals (:revs-many chd))))
     dbs)))

(defn datom-ids [chd]
  (let [cid (get-in chd [:# :id])
        rfs-fn (fn [rids k]
                 (map (fn [rid] [:db/add cid k rid]) rids))
        rvs-fn (fn [rids k]
                 (map (fn [rid] [:db/add rid k cid]) rids))]
    (concat
     (mapcat datom-ids (vals (:refs-one chd)))
     (mapcat datom-ids (vals (:revs-one chd)))
     (mapcat datom-ids (apply concat (vals (:refs-many chd))))
     (mapcat datom-ids (apply concat (vals (:revs-many chd))))
     (mapcat rfs-fn (vals (:ref-ids chd)) (keys (:ref-ids chd)))
     (mapcat rvs-fn (vals (:rev-ids chd)) (keys (:rev-ids chd))))))

(defn datom-tree [chd]
  (let [ref-ones (map (fn [k rf]
                        (let [[trunk & rest] (datom-tree rf)]
                          [k trunk rest]))
                      (keys (:refs-one chd))
                      (vals (:refs-one chd)))
        rev-link  (if-let [rid (get-in chd [:# :rid])]
                    [{:db/id (get-in chd [:# :id]) (get-in chd [:# :rkey]) rid}]
                    [])
        trunk (-> (into {}
                        (map (fn [k rfs]
                               [k (set (mapcat datom-tree rfs))])
                             (keys (:refs-many chd))
                             (vals (:refs-many chd))))
                  (merge (into {}
                               (map (fn [[k trunk]] [k trunk]) ref-ones)))
                  (merge (:data-one chd))
                  (merge (:data-many chd)))
        tarr     (if-not (empty? trunk)
                   (let [trunk    (if-let [id (get-in chd [:# :id])]
                                    (assoc trunk :db/id id) trunk)]
                     [trunk])
                   [])
        revs     (mapcat datom-tree (apply concat (vals (:revs-many chd))))
        _        (swap! *nested* concat revs)]
    (concat
     tarr
     rev-link
     (mapcat (fn [[_ _ rest]] rest) ref-ones))))

(defn wrap-check-empty [f]
  (fn [chdata]
    (if (and (db-id? (get-in chdata [:# :id]))
             (empty? (:data-one chdata))
             (empty? (:data-many chdata))
             (empty? (:refs-one chdata))
             (empty? (:refs-many chdata)))
      (error "WRAP_CHECK_EMPTY: Cannot allow an empty ref for: " chdata)
      (f chdata))))

(defn datoms-raw
  [chdata]
  (binding [*nested* (atom [])]
    (vec (concat
          ((wrap-check-empty datom-tree) chdata)
          @*nested*
          (datom-ids chdata)
          (datom-funcs chdata)))))

(defn datoms [datasource]
  (let [chdata (-> datasource :process :characterised)
        ndata (datoms-raw chdata)]
    (assoc-in datasource [:process :emitted] ndata)))
