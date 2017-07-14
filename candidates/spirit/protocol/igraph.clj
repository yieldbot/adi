(ns spirit.protocol.igraph)

(defprotocol IGraph
  (-install-schema  [db schema])
  (-empty   [db opts])
  (-select  [db selector opts])
  (-insert  [db data opts])
  (-delete  [db selector opts])
  (-retract [db selector key opts])
  (-update  [db selector data opts]))
