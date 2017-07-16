(ns spirit.data.keystore
  (:require [spirit.protocol.ikeystore :as keystore]
            [spirit.data.keystore.base]
            [hara.component :as component]))

(defn put-in
  "adds data to the database
 
   (-> (keystore)
       (put-in [:b] {:d 2})
       (put-in [:b] {:c 2})
       (peek-in))
   => {:b {:d 2, :c 2}}"
  {:added "0.5"}
  ([db v] (put-in db [] v))
  ([db path v]
   (keystore/-put-in db path v)))

(defn peek-in
  "accesses all or part of the database
 
   (-> (keystore)
       (put-in {:a 1, :b {:c 2}})
       (peek-in [:b]))
   => {:c 2}"
  {:added "0.5"}
  ([db] (peek-in db []))
  ([db path]
   (keystore/-peek-in db path)))

(defn keys-in
  "returns all keys in the particular level
 
   (-> (keystore)
       (put-in {:a 1, :b {:c 2}})
       (keys-in)
       (sort))
   => [:a :b]"
  {:added "0.5"}
  ([db] (keys-in db []))
  ([db path]
   (keystore/-keys-in db path)))

(defn drop-in
  "removes keys in the database
   
   (-> (keystore)
       (put-in {:a 1, :b {:c 2}})
       (drop-in [:a])
       (peek-in))
   => {:b {:c 2}}"
  {:added "0.5"}
  ([db] (drop-in db []))
  ([db path]
   (keystore/-drop-in db path)))

(defn set-in
  "adds data to the database. overwrites
 
   (-> (keystore)
       (set-in [:b] {:d 2})
       (set-in [:b] {:c 2})
       (peek-in))
   => {:b {:c 2}}"
  {:added "0.5"}
  ([db v] (set-in db [] v))
  ([db path v]
   (keystore/-set-in db path v)))

(defn select-in
  "selects the key values corresponding to the query
   (-> (keystore)
       (set-in [:data] {:a 1
                        :b 2
                        :c 3
                        :d 4})
       (select-in [:data] odd?))
   => [[:a 1] [:c 3]]"
  {:added "0.5"}
  ([db v] (select-in db [] v))
  ([db path v]
   (keystore/-select-in db path v)))

(defn batch-in
  "batch add and removal of data
 
   (-> (keystore)
       (put-in {:data {:a 1
                       :b 2}
                :input {:c 3
                        :d 4}})
       (batch-in {:data {:x 1}
                 :input {:y 2}}
                 [[:data :a] [:input :d]])
       (peek-in))
   => {:data {:b 2, :x 1}
       :input {:c 3, :y 2}}"
  {:added "0.5"}
  ([db add-map]
   (batch-in db [] add-map []))
  ([db add-map remove-vec]
   (batch-in db [] add-map remove-vec))
  ([db path add-map remove-vec]
   (keystore/-batch-in db path add-map remove-vec)))

(defn create
  "creates a keystore that is component compatible
 
   (create {:type :mock
            :output {:file \"test.edn\"
                     :reset true}})
   ;;=> #keystore.mock<uninitiased>
   "
  {:added "0.5"}
  [m]
  (keystore/create m))

(defn keystore
  "creates a keystore and 
 
   (keystore {:type :mock
              :data {:hello :world}
              :file {:path \"test.edn\"
                     :reset true}})
   => MockKeystore  ;; #keystore.mock{}
   "
  {:added "0.5"}
  ([]
   (keystore {:type :mock}))
  ([m]
   (-> (keystore/create m)
       (component/start))))
