(ns spirit.core.depack
  (:require [spirit.process.unpack :as unpack]
            [spirit.data
             [common :refer :all]
             [checks :refer [db-id?]]]
            [hara.common
             [checks :refer [long? hash-map?]]
             [error :refer [error]]]
            [hara.data.map :refer [assoc-in-if]]
            [hara.string.path :as path]))

(declare depack)

(defn depack-ref
  [data attr spirit]
  (let [ns  (-> attr :ref :ns)]
    (cond (nil? data) (error "CANNOT be NIL:" attr)

          (hash-map? data)
          (unpack/strip-ns (depack data spirit) ns)

          (long? data) data

          (db-id? data) (iid-seed data)

          :else
          (error "RETURN_REF: Cannot process data: " data))))

(defn wrap-depack-sets [f]
  (fn [data attr spirit]
    (cond (set? data)
          (->> data
               (map #(f % attr spirit))
               (filter identity)
               (set))
          :else (f data attr spirit))))

(defn depack-loop
  [data spirit]
  (reduce-kv
   (fn [out k v]
     (if-let [[attr] (-> spirit :schema :flat (get k))]
       (cond (= :ref (-> attr :type))
             (assoc-in-if out (path/split k)
                          ((wrap-depack-sets depack-ref) (get data k) attr spirit))


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
  [data spirit]
  (cond (vector? data)
        (mapv #(depack % spirit) data)

        (hash-map? data)
        (depack-loop data spirit)

        :else data))
