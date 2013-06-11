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
    (as/verify (-> ds :schema :fgeni))
    (if install?
      (init-schema ds))
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
  (aa/insert! (:conn ds) data ds))

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
                show-refs follow-refs]} opts]
    (assoc ds :reap {:data (if hide-data :hide :show)
                     :refs (cond follow-refs   :follow
                                 show-refs     :show
                                 :else         :hide)
                     :ids  (if hide-ids :hide :show)}
           :view (if (vector? view)
                   (vec (concat (select-view-val val) view))
                   (conj (vec (select-view-val val)) view)))))

(defn select-call [ds val args f]
  (let [opts (into {} (u/auto-pair-seq args))
        {:keys [at first]} opts]
    (-> ds
        (select-at at)
        (f val args)
        (select-first first))))

(defn select [ds val & args]
  (select-call ds val args
               (fn [val ds opts]
                 (aa/select val (select-view ds val opts)))))

(defn select [ds val & args]
  (let [opts (into {} (u/auto-pair-seq args))
        {:keys [at first]} opts]
    (-> (select-at ds at)
        (aa/select val (select-view ds val opts))
        (select-first first))))

(defn select-ids [ds val & args]
  (let [opts (into {} (u/auto-pair-seq args))
        {:keys [at first]} opts]
    (-> (select-at ds at)
        (aa/select-ids val ds)
        (select-first first))))

(defn select-entities [ds val & args]
  (let [opts (into {} (u/auto-pair-seq args))
        {:keys [at first]} opts]
    (-> (select-at ds at)
        (aa/select-entities val ds)
        (select-first first))))

(defn select-fields [ds val fields & args]
  (let [opts (into {} (u/auto-pair-seq args))
        {:keys [at first]} opts]
    (-> (select-at ds at)
        (aa/select-fields val fields ds)
        (select-first first))))

(defn delete! [ds val & args]
  (aa/delete! (:conn ds) val (merge-args ds args)))

(defn update! [ds val data & args]
  (aa/update! (:conn ds) val data (merge-args ds args)))

(defn retract! [ds val ks & args]
  (aa/retract! (:conn ds) val ks (merge-args ds args)))

(defn transactions
  ([ds attr]
      (->> (d/q '[:find ?tx
                  :in $ ?a
                  :where [_ ?a ?v ?tx _]
                  [?tx :db/txInstant ?tx-time]]
                (d/history (d/db (:conn ds)))
                attr)
           (map #(first %))
           (map d/tx->t)
           (sort)))
  ([ds attr val]
      (->> (d/q '[:find ?tx
                  :in $ ?a ?v
                  :where [_ ?a ?v ?tx _]
                  [?tx :db/txInstant ?tx-time]]
                (d/history (d/db (:conn ds)))
                attr val)
           (map #(first %))
           (map d/tx->t)
           (sort))))
