(ns adi.core
  (:use [adi.emit.process :only [process-init-env]]
        [hara.common :only [hash-map? hash-set?]]
        hara.hash-map)
  (:require [adi.schema :as as]
            [adi.utils :as u]
            [adi.api :as aa]
            [adi.api.schema :as aas]
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

(defn- select-at [ds t]
  (let [db (d/db (:conn ds))]
    (if t (d/as-of db t) db)))

(defn select-first [res first]
  (if first (clojure.core/first res) res))


(defn select-view-val [val]
  (cond (keyword? val)  (vector (or (keyword-root val) val))
        (hash-map? val) (set (map #(or (keyword-root %) %)
                                  (keys (flatten-keys-nested val))))
        (hash-set? val) (set (mapcat select-view-val val))))

(defn select-view [ds val opts]
  (let [{:keys [view hide-ids hide-data
                hide-refs follow-refs]} opts]
    ;;(println (vec (concat (select-view-val val) view)))
    (assoc ds :reap {:data (if hide-data :hide :show)
                     :refs (cond follow-refs   :follow
                                 hide-refs     :hide
                                 :else         :show)
                     :ids  (if hide-ids :hide :show)}
           :view (if (vector? view)
                   (vec (concat (select-view-val val) view))
                   (conj (vec (select-view-val val)) view)))))

(defn select [ds val & args]
  (let [opts (into {} (u/auto-pair-seq args))
        {:keys [at first]} opts
        res (aa/select (select-at ds at) val (select-view ds val opts))]
    ;;(println (:view (select-view ds val opts)))
    (select-first res first)))

(defn select-ids [ds val & args]
  (aa/select-ids (d/db (:conn ds)) val (merge-args ds args)))

(defn select-entities [ds val & args]
  (aa/select-entities (d/db (:conn ds)) val (merge-args ds args)))

(defn select-fields [ds val fields & args]
  (aa/select-fields (d/db (:conn ds)) val fields (merge-args ds args)))

(defn delete! [ds val & args]
  (aa/delete! (:conn ds) val (merge-args ds args)))

(defn update! [ds val data & args]
  (aa/update! (:conn ds) val data (merge-args ds args)))

(defn retract! [ds val ks & args]
  (aa/retract! (:conn ds) val ks (merge-args ds args)))


(comment

  (defn select-first-entity [ds val & args]
    (first (select-entities (merge-args ds args) val)))



         (defn select-first [ds val & args]
           (first (apply select ds val args)))

         #_(defn delete-all! [val ds & args]
             (let [rrs  (or (aa/emit-ref-set (:fgeni ds)))]
               (aa/delete! (:conn ds) val (-> (merge-args ds args)
                                              (into [[:ref-set rrs]])))))

                                              )
