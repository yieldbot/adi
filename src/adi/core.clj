(ns adi.core
  (:use adi.utils)
  (:require [adi.data :as ad]
            [adi.schema :as as]
            [adi.api :as aa]
            [datomic.api :as d]))

(defn init-schema [ds]
  (aa/install-schema (:geni ds) (:conn ds) ))

(defn datastore [uri geni & [install? recreate?]]
  (let [geni (as/add-idents geni)
        ds {:conn   (aa/connect! uri recreate?)
            :geni   geni
            :fgeni  (flatten-all-keys geni)}]
    (if install? (init-schema ds))
    ds))

(defn transact [data ds]
  (d/transact (:conn ds) data))

(defn q
  [qu args ds]
  (apply d/q qu (d/db (:conn ds)) args))

(defn- merge-args
  ([ds args]
     (let [pargs (partition 2 args)]
       (merge-args nil pargs ds)))
  ([_  pargs output]
     (if-let [[k v] (first pargs)]
       (merge-args nil (next pargs) (assoc output k v))
       output)))

(defn select-ids [val ds & args]
  (aa/select-ids val (d/db (:conn ds)) (merge-args ds args)))

(defn select-entities [val ds & args]
  (aa/select-entities val (d/db (:conn ds)) (merge-args ds args)))

(defn select-first-entity [val ds & args]
  (first (select-entities val (merge-args ds args))))

(defn select [val ds & args]
  (aa/select val (d/db (:conn ds)) (merge-args ds args)))

(defn select-first [val ds & args]
  (first (apply select val ds args)))

(defn delete! [val ds & args]
  (aa/delete! val (:conn ds) (merge-args ds args)))

(defn delete-all! [val ds & args]
  (let [rrs  (or (aa/emit-ref-set (:fgeni ds)))]
    (aa/delete! val (:conn ds) (-> (merge-args ds args)
                                   (into [[:ref-set rrs]])))))

(defn insert! [data ds & args]
  (aa/insert! data (:conn ds) (merge-args ds args)))

(defn update! [val data ds & args]
  (aa/update!  val data (:conn ds) (merge-args ds args)))

(defn retract! [val ks ds & args]
  (aa/retract!  val ks (:conn ds) (merge-args ds args)))
