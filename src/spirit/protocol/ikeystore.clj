(ns spirit.protocol.ikeystore)

(defprotocol IKeystore
  (-put-in    [store path v])
  (-peek-in   [store arr])
  (-keys-in   [store arr])
  (-drop-in   [store arr])
  (-set-in    [store arr v])
  (-select-in [store arr v])
  (-mutate-in [store ks add-map del-vec]))

(defmulti create
  "creates a keystore
 
   (create {:type :atom})
 
   (create {:type :mock
            :file \"test.edn\"})"
  {:added "0.5"}
  :type)
