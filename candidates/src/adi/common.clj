(ns adi.common
  (:require [datomic.api :as d]))

(defn iid
  "Constructs a new id"
  ([] (d/tempid :db.part/user))
  ([obj]
     (let [v (if (number? obj) (long obj) (hash obj))
           v (if (< 0 v) (- v) v)]
       (d/tempid :db.part/user v ))))

(defn isym
 "Returns a new datomic symbol with a unique name. If a prefix string
  is supplied, the name is `prefix#` where `#` is some unique number. If
  prefix is not supplied, the prefix is `e_`.

   (isym) ;=> ?e_1238

   (isym) ;=> ?e_1241

   (isym \"v\") ;=> ?v1250
  "
 ([] (isym 'e_))
 ([prefix] (symbol (str "?" (name (gensym prefix))))))

(defn incremental-sym-gen
  ([s] (incremental-sym-gen s 0))
  ([s n]
     (let [r (atom n)]
       (fn []
         (swap! r inc)
         (symbol (str "?" s @r))))))

(defn incremental-id-gen
  ([] (incremental-id-gen 0))
  ([n]
     (let [r (atom n)]
       (fn []
         (swap! r inc)
         @r))))
