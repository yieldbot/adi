(ns spirit.core.datomic.api.depack
  (:require [spirit.core.datomic.process.unpack :as unpack]
            [spirit.core.datomic.data :as data]
            [spirit.core.datomic.data.checks :as checks]
            [hara.common
             [checks :refer [long? hash-map?]]
             [error :refer [error]]]
            [hara.data.map :refer [assoc-in-if]]
            [hara.string.path :as path]))

(declare depack)

(defn depack-ref
  [data attr datasource]
  (let [ns  (-> attr :ref :ns)]
    (cond (nil? data) (error "CANNOT be NIL:" attr)

          (hash-map? data)
          (unpack/strip-ns (depack data datasource) ns)

          (long? data) data

          (checks/db-id? data) (data/iid-seed data)

          :else
          (error "RETURN_REF: Cannot process data: " data))))

(defn wrap-depack-sets [f]
  (fn [data attr datasource]
    (cond (set? data)
          (->> data
               (map #(f % attr datasource))
               (filter identity)
               (set))
          :else (f data attr datasource))))

(defn depack-loop
  [data datasource]
  (reduce-kv
   (fn [out k v]
     (if-let [[attr] (-> datasource :schema :flat (get k))]
       (cond (= :ref (-> attr :type))
             (assoc-in-if out (path/split k)
                          ((wrap-depack-sets depack-ref) (get data k) attr datasource))


             (= :enum (-> attr :type))
             (assoc-in-if out (path/split k)
                          (if-let [ens (-> attr :enum :ns)]
                            (-> data (get k) path/split last)
                            (get data k)))

             :else
             (assoc-in-if out (path/split k) (get data k)))
       (assoc out k v)))
   {} data))

(defn depack
  [data datasource]
  (cond (vector? data)
        (mapv #(depack % datasource) data)

        (hash-map? data)
        (depack-loop data datasource)

        :else data))
