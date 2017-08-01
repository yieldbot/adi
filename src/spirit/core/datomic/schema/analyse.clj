(ns spirit.core.datomic.schema.analyse
  (:require [hara.data.path :as data]
            [hara.string.path :as path]
            [spirit.core.datomic.schema.base :as base]))

(defn analyse-single [datum]
  (let [path (-> datum :db/ident path/split)
        attrs (reduce-kv (fn [out dbk v]
                           (let [k (path/val dbk)
                                 k (if (= k :valueType) :type k)
                                 v (if (#{:type :cardinality :unique} k)
                                     (path/val v)
                                     v)]
                             (if (= (-> base/datomic-specific k :default) v)
                               out
                               (assoc out k v))))
                         {}
                         (dissoc datum :db/ident))]
    [path attrs]))

(defn analyse [datums]
  (reduce (fn [out d]
            (let [[path attrs] (analyse-single d)]
              (assoc-in out path [attrs])))
          {}
          datums))
