(ns adi.api.schema
  (:use [hara.common :only [hash-set? hash-map?]]
        [hara.hash-map :only [treeify-keys keyword-stem]])
  (:require [datomic.api :as d]
            [hara.hash-map :as h]))

(defn schema-properties [ds]
  (let [data (d/q '[:find ?ident ?type ?cardinality ?e :where
                    [?e :db/ident ?ident]
                    [?e :db/valueType ?t]
                    [?t :db/ident ?type]
                    [?e :db/cardinality ?c]
                    [?c :db/ident ?cardinality]]
                  (d/db (ds :conn)))]
    (zipmap (map first data) data)))

(defn schema-idents [ds]
  (d/q '[:find ?ident ?e :where
         [?e :db/ident ?ident]]
       (d/db (ds :conn))))

(defn make-enum [[ident id]]
  {:db/ident ident
   :db/id id})

(defn filter-keys [m nss]
  (cond (keyword? nss)
        (select-keys m [nss])

        (hash-set? nss)
        (select-keys m nss)))

(defn schema-enums
  ([ds]
     (let [data (schema-idents ds)]
       (treeify-keys
        (-> (zipmap (map first data)
                    (map make-enum data))
            (dissoc (keys (schema-properties ds)))))))
  ([ds nss]
     (filter-keys (schema-enums ds) nss)))

(defn schema
  ([ds]
      (treeify-keys
       (into {}
             (map (fn [[k [ident type car]]]
                    [ident [{:ident ident
                             :type (keyword-stem type)
                             :cardinality (keyword-stem car)}]])
                  (schema-properties ds)))))
  ([ds nss]
     (filter-keys (schema ds) nss)))

(defn schema-namespaces [ds]
  (keys (schema ds)))

(defn schema-enum-namespaces [ds]
  (keys (schema-enums ds)))

(defn schema-nss [ds nss]
  (->> (h/keyword-split nss)
       (apply list :schema :geni)
       (get-in ds)))

(defn schema-required-keys
  ([res]
     (cond (vector? res)
           [((fn [[prop]]
               (if (:required prop) (:ident prop)))
             res)]

           (hash-map? res)
           (mapcat #(schema-required-keys %) (vals res))))
  ([ds nss]
     (-> (schema-required-keys (schema-nss ds nss))
         (set)
         (disj nil))))

(defn schema-property? [ds nss]
  (vector? (schema-nss ds nss)))
