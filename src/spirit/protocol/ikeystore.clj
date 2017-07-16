(ns spirit.protocol.ikeystore)

(defprotocol IKeystore
  (-put-in    [store path v])
  (-peek-in   [store path])
  (-keys-in   [store path])
  (-drop-in   [store path])
  (-set-in    [store path v])
  (-select-in [store path v])
  (-batch-in  [store path add-map remove-vec]))

(defmulti create :type)
