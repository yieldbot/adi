(ns spirit.data.cache
  (:require [spirit.protocol.icache :as cache]
            [spirit.data.cache.base :as base]
            [hara.component :as component])
  (:refer-clojure :exclude [get set keys count]))

(defn set
  ([cache key value]
   (cache/-set cache key value))
  ([cache key value expiry]
   (cache/-set cache key value expiry)))

(defn get
  [cache key]
  (cache/-get cache key))

(defn count    [cache]
  (cache/-count cache))

(defn batch
  ([cache add-map]
   (cache/-batch cache add-map))
  ([cache add-map remove-vec]
   (cache/-batch cache add-map remove-vec)))

(defn delete   [cache key]
  (cache/-delete cache key))

(defn clear    [cache]
  (cache/-clear cache))

(defn all      [cache]
  (cache/-all cache))

(defn keys     [cache]
  (cache/-keys cache))

(defn touch    [cache key expiry]
  (cache/-touch cache key expiry))

(defn expired? [cache key]
  (cache/-expired? cache key))

(defn expiry   [cache key]
  (cache/-expiry cache key))

(defn create [m]
  (cache/create m))

(defn cache [m]
  (-> (cache/create m)
      (component/start)))
