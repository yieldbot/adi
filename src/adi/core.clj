(ns adi.core
  (:use adi.utils)
  (:require [adi.data :as ad]
            [adi.schema :as as]
            [adi.api :as aa]
            [datomic.api :as d]))

(defn init-scheme-map [ds]
  (d/transact (:conn ds) (as/emit-schemas (:fsm ds))))

(defn datastore [sm uri & [install? recreate?]]
  (if recreate? (d/delete-database uri)
      (d/create-database uri))
  (let [ds {:conn (d/connect uri)
            :fsm   (flatten-keys sm)}]
    (if install? (init-scheme-map ds))
    ds))

(defn transact [ds data]
  (d/transact (:conn ds) data))

(defn query
  [ds qu & args]
  (apply d/q qu (d/db (:conn ds)) args))

(defn select-ids [ds val]
  (aa/select-ids (d/db (:conn ds)) val))

(defn select-entities [ds val]
  (aa/select-entities (d/db (:conn ds)) val))

(defn select [ds val & [rset]]
  (aa/select (d/db (:conn ds)) (:fsm ds) val (or rset #{})))

(defn delete! [ds val & [rset]]
  (aa/delete! (:conn ds) val (or rset #{})))

(defn delete-all! [ds val]
  (let [rs (as/rset (:fsm ds))]
    (aa/delete! (:conn ds) val rs)))

(defn insert! [ds data & more]
  (apply aa/insert! (:conn ds) (:fsm ds) data more))

(defn update! [ds val data]
  (aa/update! (:conn ds) (:fsm ds) val data))

(defn retract! [ds val ks]
  (aa/retract! (:conn ds) val ks))
