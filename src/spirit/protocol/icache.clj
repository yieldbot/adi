(ns spirit.protocol.icache)

(defprotocol ICache
  (-set      [cache key value]
             [cache key value expiry])
  (-get      [cache key])
  (-count    [cache])
  (-batch    [cache add-values add-expiry remove-vec])
  (-delete   [cache key])
  (-clear    [cache])
  (-all      [cache])
  (-keys     [cache])
  (-touch    [cache key expiry])
  (-expired? [cache key])
  (-expiry   [cache key]))
  
(defmulti create :type)
