(ns adi.data.unpack
  (:require [hara.common :refer [error hash-set? hash-map? dissoc-in
                                 assoc-in-if keyword-split]]
            [hara.collection.hash-map :refer [treeify-keys]]
            [datomic.api :as d]))

(declare unpack)

(defn strip-ns [m ns]
  (let [nsv (keyword-split ns)
        nm  (get-in m nsv)
        xm  (dissoc-in m nsv)]
    (if (empty? xm)
      nm
      (assoc nm :+ xm))))

(defn unpack-ref
  [ent rf attr v env]
  (let [ns  (-> attr :ref :ns)]
    (cond (nil? rf) nil

          (= v :id)
          (:db/id ent)

          (hash-map? v)
          (strip-ns (unpack rf v env) ns)

          (= v :yield)
          (if (not (@(:seen-ids env) (:db/id ent)))
            (strip-ns (unpack rf env) ns)
            {:+ {:db {:id (:db/id ent)}}})

          :else
          (error "RETURN_REF: Cannot process directive: " v))))

(defn wrap-db-id [f]
  (fn [ent model fmodel env]
    (let [output (f ent model fmodel env)
          id     (:db/id ent)
          _      (swap! (:seen-ids env) conj id)]
      (if (-> env :options :ids)
        (assoc-in output [:db :id] id)
        output))))

(defn wrap-unpack-sets [f]
  (fn [ent attr v env]
    (let [rf (get ent (-> attr :ref :key))]
      (cond (hash-set? rf)
            (set (filter identity (map #(f ent % attr v env) rf)))
            :else (f ent rf attr v env)))))

(defn unpack-loop
  ([ent model fmodel env]
     (unpack-loop ent fmodel fmodel env {}))
  ([ent model fmodel env output]
     (if-let [[k v] (first model)]
       (if-let [[attr] (-> env :schema :flat (get k))]
         (let [noutput (cond (= v :unchecked)
                             output

                             (= :ref (-> attr :type))
                             (assoc-in-if output (keyword-split k)
                                          ((wrap-unpack-sets unpack-ref) ent attr v env))

                             :else
                             (assoc-in-if output (keyword-split k) (get ent k)))]
           (recur ent (next model) fmodel env noutput))
         (error "RETURN: key " k " is not in schema"))
       output)))

(defn unpack
  ([ent env]
     (if-let [fmodel (-> env :model :return)]
       (unpack ent fmodel (assoc env :seen-ids (atom #{})))
       (let [ent (d/touch ent)
             ks  (filter (fn [k] (not= :ref (-> env :schema :flat k first :type)))
                         (keys ent))
             res (treeify-keys (select-keys ent ks))]
         (if (-> env :options :ids)
           (assoc-in res
                     [:db :id] (:db/id ent))
           res))))
  ([ent fmodel env]
     ((wrap-db-id unpack-loop) ent fmodel fmodel env)))
