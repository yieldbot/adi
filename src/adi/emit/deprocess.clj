(ns adi.emit.deprocess
  (:use hara.common
        hara.hash-map
        [hara.control :only [if-let]])
  (:require [adi.schema :as as])
  (:refer-clojure :exclude [if-let]))

(declare deprocess
         deprocess-fm deprocess-view
         deprocess-ref
         deprocess-assoc deprocess-assoc-data deprocess-assoc-ref)

(defn deprocess-init-env [env]
  (if-let [vw (-> env :view)
           ?  (hash-set? vw)
           vw (zipmap vw (repeat :show))]
    (assoc env :view vw)
    env))

(defn deprocess-assoc-data
  [output k v env]
  (let [vw  (or (-> env :view) {})
        dir (or (vw k)
                (-> env :deprocess :data-default)
                :hide)]
    (cond (= dir :show)
          (assoc output k v)

          :else output)))

(defn deprocess-ref
  [k rf meta env exclude]
  (let [vw  (or (-> env :view) {})
        dir (or (vw k)
                (-> env :deprocess :refs-default)
                :hide)
        id (:db/id rf)]
    (cond (= dir :hide) nil

          (exclude id) {:+ {:db {:id id}}}

          (= dir :ids) (if id {:+ {:db {:id id}}})

          (= dir :show)
          (let [rns (keyword-split (-> meta :ref :ns))
                nm  (deprocess rf env exclude)
                cm  (get-in nm rns)
                xm  (dissoc-in nm rns)]
            (if (empty? xm) (or cm {})
                (merge cm {:+ xm}))))))

(defn deprocess-assoc-ref
  [output k v meta env exclude]
  (let [c   (or (:cardinality meta) :one)]
    (cond (= c :one)
          (if-let [rout (deprocess-ref k v meta env exclude)]
            (assoc output k rout)
            output)

          (= c :many)
          (assoc output k
                    (-> (map #(deprocess-ref k % meta env exclude) v)
                        (set)
                        (disj nil))))))

(defn deprocess-assoc
  ([output k v meta env exclude]
     (let [t   (:type meta)]
       (cond (not= t :ref)
             (deprocess-assoc-data output k v env)

             :else
             (deprocess-assoc-ref output k v meta env exclude)))))

(defn deprocess-fm
  ([fm env exclude]
     (deprocess-fm {} fm env exclude))
  ([output fm env exclude]
     (if-let [[k v] (first fm)]
       (if-let [[meta] (-> env :schema :fgeni k)]
         (-> output
             (deprocess-assoc k v meta env exclude)
             (recur (next fm) env exclude))
         (recur output (next fm) env exclude))
       output)))

(defn deprocess-view
  ([fm env exclude vw]
     (deprocess-view {} fm env exclude vw))
  ([output fm env exclude vw]
     (if-let [[k dir] (first vw)]
       (if-let [[meta] (-> env :schema :fgeni k)
                v (or (get fm k) (get fm (-> meta :ref :key)))]
         (-> output
             (deprocess-assoc k v meta env exclude)
             (recur fm env exclude (next vw)))
         (recur output fm env exclude (next vw)))
       output)))

(defn deprocess
  ([fm env]
     (deprocess fm env #{}))
  ([fm env exclude]
     (let [id (:db/id fm)
           output  (if id {:db/id id} {})
           exclude (if id (conj exclude id) exclude)
           env     (deprocess-init-env env)
           fm-out  (deprocess-fm output fm env exclude)
           fm-ks   (keys fm-out)
           vw      (apply dissoc (or (-> env :view) {}) fm-ks)
           vw-out  (deprocess-view fm env exclude vw)]
       (treeify-keys (merge vw-out fm-out)))))
