(ns spirit.common.graph
  (:require [spirit.protocol.igraph :as graph]
            [hara.component :as component])
  (:refer-clojure :exclude [update empty]))

(defn install-schema  [db schema]
  (graph/-install-schema db schema))

(defn empty
  ([db]
   (empty db {}))
  ([db opts]
   (graph/-empty db opts)))

(defn select
  ([db selector]
   (select db selector {}))
  ([db selector opts]
   (graph/-select db selector opts)))

(defn insert
  ([db data]
   (insert db data {}))
  ([db data opts]
   (graph/-insert db data opts)))

(defn delete
  ([db selector]
   (delete db selector {}))
  ([db selector opts]
   (graph/-delete db selector opts)))

(defn update
  ([db selector data]
   (update db selector data {}))
  ([db selector data opts]
   (graph/-update db selector data opts)))

(defn retract
  ([db selector keys]
   (retract db selector keys {}))
  ([db selector keys opts]
   (graph/-retract db selector keys opts)))

(defmulti create :type)

(defn graph [m]
  (-> (create m)
      (component/start)))
