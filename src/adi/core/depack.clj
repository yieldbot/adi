(ns adi.core.depack
  (:require [adi.process.unpack :as unpack]
            [adi.data
             [common :refer :all]
             [checks :refer [db-id?]]]
            [hara.common
             [checks :refer [long? hash-map?]]
             [error :refer [error]]]
            [hara.data.map :refer [assoc-in-if]]
            [hara.string.path :as path]))

(declare depack)

(defn depack-ref
  [data attr adi]
  (let [ns  (-> attr :ref :ns)]
    (cond (nil? data) (error "CANNOT be NIL")

          (hash-map? data)
          (unpack/strip-ns (depack data adi) ns)

          (long? data) data

          (db-id? data) (iid-seed data)

          :else
          (error "RETURN_REF: Cannot process data: " data))))

(defn wrap-depack-sets [f]
  (fn [data attr adi]
    (cond (set? data)
          (->> data
               (map #(f % attr adi))
               (filter identity)
               (set))
          :else (f data attr adi))))

(defn depack-loop
  [data adi]
  (reduce-kv
   (fn [out k v]
     (if-let [[attr] (-> adi :schema :flat (get k))]
       (cond (= :ref (-> attr :type))
             (assoc-in-if out (path/split k)
                          ((wrap-depack-sets depack-ref) (get data k) attr adi))


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
  [data adi]
  (cond (vector? data)
        (mapv #(depack % adi) data)

        (hash-map? data)
        (depack-loop data adi)

        :else data))
