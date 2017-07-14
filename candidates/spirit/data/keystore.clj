(ns spirit.data.keystore
  (:require [spirit.protocol.ikeystore :as keystore]
            [hara.component :as component]))

(defn put-in
  "adds data to the database
 
   (-> (create {:type :mock})
       (put-in [:b] {:d 2})
       (put-in [:b] {:c 2})
       (peek-in))
   => {:b {:d 2, :c 2}}"
  {:added "0.5"}
  ([db v] (put-in db [] v))
  ([db arr v]
    (keystore/-put-in db arr v)
    db))

(defn peek-in
  "accesses all or part of the database
 
   (-> (create {:type :mock})
       (put-in {:a 1, :b {:c 2}})
       (peek-in [:b]))
   => {:c 2}"
  {:added "0.5"}
  ([db] (peek-in db []))
  ([db arr]
    (keystore/-peek-in db arr)))

(defn keys-in
  "returns all keys in the particular level
 
   (-> (create {:type :mock})
       (put-in {:a 1, :b {:c 2}})
       (keys-in)
       (sort))
   => [:a :b]"
  {:added "0.5"}
  ([db] (keys-in db []))
  ([db arr]
    (keystore/-keys-in db arr)))

(defn drop-in
  "removes keys in the database
   
   (-> (create {:type :mock})
       (put-in {:a 1, :b {:c 2}})
       (drop-in [:a])
       (peek-in))
   => {:b {:c 2}}"
  {:added "0.5"}
  ([db] (drop-in db []))
  ([db arr]
    (keystore/-drop-in db arr)
    db))

(defn set-in
  "adds data to the database. overwrites
 
   (-> (create {:type :mock})
       (set-in [:b] {:d 2})
       (set-in [:b] {:c 2})
       (peek-in))
   => {:b {:c 2}}"
  {:added "0.5"}
  ([db v] (set-in db [] v))
  ([db arr v]
     (keystore/-set-in db arr v)
     db))

(defn select-in
  "selects the key values corresponding to the query
   (-> (create {:type :mock})
       (set-in [:data] {:a 1
                        :b 2
                        :c 3
                        :d 4})
       (select-in [:data] odd?))
   => [[:a 1] [:c 3]]"
  {:added "0.5"}
  ([db v] (select-in db [] v))
  ([db arr v]
    (keystore/-select-in db arr v)))

(defn mutate-in
  "batch add and removal of data
 
   (-> (create {:type :mock})
       (put-in {:data {:a 1
                       :b 2}
                :input {:c 3
                        :d 4}})
       (mutate-in []
                 {:data {:x 1}
                   :input {:y 2}}
                  [[:data :a] [:input :d]])
       (peek-in))
   => {:data {:b 2, :x 1}
       :input {:c 3, :y 2}}"
  {:added "0.5"}
  [db ks add-map del-vec]
  (keystore/-mutate-in db ks add-map del-vec)
  db)

(defn create
  [m]
  (keystore/create m))

(defn keystore
  "creates a standalone keystore
 
   (keystore)
   => MockKeystore
 
   (keystore {:type :atom})
   => clojure.lang.Atom"
  {:added "0.5"}
  ([m]
   (-> (keystore/create m)
       (component/start))))
