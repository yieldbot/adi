(ns adi.core
  (:use adi.utils)
  (:require [adi.data :as ad]
            [adi.emit :as ae]
            [adi.schema :as as]
            [adi.api :as aa]
            [datomic.api :as d]))

(defn init-schema [ds]
  (aa/install-schema (:conn ds) (:fschm ds)))

(defn datastore [uri schm & [install? recreate?]]
  (let [ds {:conn   (aa/connect! uri recreate?)
            :fschm  (flatten-keys schm)}]
    (if install? (init-schema ds))
    ds))

(defn transact [ds data]
  (d/transact (:conn ds) data))

(defn q
  [ds qu args]
  (apply d/q qu (d/db (:conn ds)) args))

(defn select-ids [ds val]
  (aa/select-ids (d/db (:conn ds)) (:fschm ds) val))

(defn select-entities [ds val]
  (aa/select-entities (d/db (:conn ds)) val))

(defn select-first-entity [ds val]
  (first (select-entities ds val)))

(defn select [ds val & [rrs]]
  (aa/select (d/db (:conn ds)) (:fschm ds) val (or rrs #{})))

(defn select-first [ds val & [rrs]]
  (first (select ds val rrs)))

(defn delete! [ds val & [rrs]]
  (aa/delete! (:conn ds) val (or rrs #{})))

(defn delete-all! [ds val]
  (let [rrs (ae/emit-refroute (:fschm ds))]
    (aa/delete! (:conn ds) val rrs)))

(defn insert! [ds data]
  (aa/insert! (:conn ds) (:fschm ds) data))

(defn update! [ds val data]
  (aa/update! (:conn ds) (:fschm ds) val data))

(defn retract! [ds val ks]
  (aa/retract! (:conn ds) val ks))
