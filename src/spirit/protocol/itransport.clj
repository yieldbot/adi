(ns spirit.protocol.itransport)

(defprotocol IConnection
  (-request [conn package])
  (-send    [conn package]))