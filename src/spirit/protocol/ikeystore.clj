(ns spirit.protocol.ikeystore)

(defprotocol IKeystore
  (-put-in    [db path v])
  (-peek-in   [db arr])
  (-keys-in   [db arr])
  (-drop-in   [db arr])
  (-set-in    [db arr v])
  (-select-in [db arr v])
  (-mutate-in [db ks add-map del-vec]))
