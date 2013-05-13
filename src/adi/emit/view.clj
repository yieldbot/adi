(ns adi.emit.view
  (:use adi.utils
        [hara.control :only [if-let]]
        hara.common
        hara.hash-map)
  (:require [adi.schema :as as])
  (:refer-clojure :exclude [if-let]))

(declare view)

(defn view-nval
  [fgeni k cfg]
  (if-let [[meta] (fgeni k)
           t      (:type meta)]
    (cond (not= :ref t)
          (or (cfg :data) :show)

          (= :forward (-> meta :ref :type))
          (or (cfg :refs) :ids)

          (= :reverse (-> meta :ref :type))
          (or (cfg :revs) :hide))))

(defn view-loop
  ([fgeni ks cfg]
     (view-loop fgeni ks cfg {}))
  ([fgeni ks cfg output]
     (if-let [k (first ks)]
       (if-let [nval (view-nval fgeni k cfg)]
         (recur fgeni (next ks) cfg (assoc output k nval))
         (recur fgeni (next ks) cfg output))
       output)))

(defn view-keyword [fgeni ns cfg]
  (let [nsks (hashmap-keys fgeni ns)
        ks   (keys (select-keys fgeni nsks))]
    (view-loop fgeni ks cfg)))

(defn view-hashset [fgeni st cfg]
  (apply merge-nested (map #(view-keyword fgeni % cfg) st)))

(defn view-hashmap [fgeni m cfg]
  (let [fks (keys fgeni)
        ks  (set (keys (treeify-keys m)))
        st  (view-hashset fgeni ks cfg)]
    (merge st (select-keys (flatten-keys-nested m) fks))))

(defn view
  ([fgeni] (view fgeni (hashmap-ns fgeni)))
  ([fgeni val] (view fgeni val {}))
  ([fgeni val cfg]
     (cond (keyword? val)
           (view-keyword fgeni val cfg)

           (hash-set? val)
           (view-hashset fgeni val cfg)

           (hash-map? val)
           (view-hashmap fgeni val cfg))))

(defn view-cfg
  [fgeni cfg] (view fgeni (hashmap-ns fgeni) cfg))

(defn view-make-set [view]
  (->> view
       (filter (fn [[k v]] (= v :show)))
       (map first)
       (set)))
