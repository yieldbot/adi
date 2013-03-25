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

(defn select-ids [val ds]
  (aa/select-ids val (d/db (:conn ds)) ds))

(defn select-entities [val ds]
  (aa/select-entities val (d/db (:conn ds)) ds))

(defn select-first-entity [val ds]
  (first (select-entities val ds)))

(defn select [val ds & args]
  (aa/select (d/db (:conn ds)) (into ds (partition 2 args))))

(defn select-first [val ds & args]
  (first (apply select val ds args)))

(defn delete! [val ds & args]
  (aa/delete! (:conn ds) val (into ds (partition 2 args))))

(defn delete-all! [val ds]
  (let [rrs (aa/emit-refroute (:fgeni ds))]
    (aa/delete! (:conn ds) val (into ds [[:ref-routes rrs]]))))

(defn insert! [data ds]
  (aa/insert! data (:conn ds) ds))

(defn update! [val data ds]
  (aa/update!  val data (:conn ds) ds))

(defn retract! [val ks ds]
  (aa/retract!  val ks (:conn ds) ds))
