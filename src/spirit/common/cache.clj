(ns spirit.common.cache
  (:require [spirit.protocol.icache :as interface]))

(defrecord MockCache [state]

  Object
  (toString [cache]
    (str "#cache" (interface/-keys cache)))
  
  interface/ICache
  (-set    [cache key value]
    (swap! state assoc key {:value value})
    cache)
  
  (-set    [cache key value expiry]
    (swap! state assoc key {:value value
                            :expiration (+ (System/currentTimeMillis) (* expiry 1000))})
    cache)
  
  (-get    [cache key]
    (let [{:keys [value expiration]} (get @state key)]
      (if (or (nil? expiration)
              (< (System/currentTimeMillis) expiration))
        value)))
        
  (-count [cache]
    (count (interface/-keys cache)))

  (-insert [cache m]
    (swap! state merge m))
  
  (-delete [cache key]
    (swap! state dissoc key)
    cache)

  (-clear [cache]
    (swap! state empty)
    cache)
  
  (-keys   [cache]
    (vec (keys (interface/-all cache))))

  (-all    [cache]
    (let [current (System/currentTimeMillis)]
      (reduce-kv (fn [m k {:keys [value expiration]}]
                   (if (or (nil? expiration)
                           (< current expiration))
                     (assoc m k value)
                     m))
       {}
       @state)))
  
  (-touch  [cache key expiry]
    (swap! state (fn [m] (if-let [{:keys [expiration]} (get m key)]
                           (if (and expiration
                                    (< (System/currentTimeMillis) expiration))
                             (assoc-in m [key :expiration]
                                       (+ (System/currentTimeMillis) (* expiry 1000)))
                             m))))
    cache)

  (-expired? [cache key]
    (if-let [{:keys [expiration]} (get @state key)]
      (if (nil? expiration)
        false
        (< expiration (System/currentTimeMillis)))))
  
  (-expiry [cache key]
    (if-let [{:keys [expiration]} (get @state key)]
      (cond (nil? expiration)
            :never

            (< expiration (System/currentTimeMillis))
            :expired

            :else
            (quot (- expiration (System/currentTimeMillis)) 1000)))))

(defmethod print-method MockCache
  [v w]
  (.write w (str v)))
