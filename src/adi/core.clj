(ns adi.core
  (:use [adi.emit.process :only [process-init-env]])
  (:require [adi.schema :as as]
            [adi.api :as aa]
            [datomic.api :as d]))

(defn init-schema [ds]
  (aa/install-schema (:conn ds) (-> ds :schema :fgeni)))

(defn datastore [uri geni & [install? recreate?]]
  (let [env  (process-init-env geni)
        conn (aa/connect! uri recreate?)
        ds   (assoc env :conn conn)]
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

(defn insert! [ds data & args]
  (aa/insert! (:conn ds) data (merge-args ds args)))

(defn select [ds val & args]
  (aa/select (d/db (:conn ds)) val (merge-args ds args)))

(defn delete! [ds val & args]
  (aa/delete! (:conn ds) val (merge-args ds args)))

(defn update! [ds val data & args]
  (aa/update! (:conn ds) val data (merge-args ds args)))

(defn retract! [ds val ks & args]
  (aa/retract! (:conn ds) val ks (merge-args ds args)))

(comment

  (defn select-ids [ds val & args]
    (aa/select-ids (d/db (:conn ds)) val (merge-args ds args)))
  (defn select-entities [ds val & args]
    (aa/select-entities (d/db (:conn ds)) val (merge-args ds args)))

  (defn select-first-entity [ds val & args]
    (first (select-entities (merge-args ds args) val)))



         (defn select-first [ds val & args]
           (first (apply select ds val args)))

         #_(defn delete-all! [val ds & args]
             (let [rrs  (or (aa/emit-ref-set (:fgeni ds)))]
               (aa/delete! (:conn ds) val (-> (merge-args ds args)
                                              (into [[:ref-set rrs]])))))

                                              )
