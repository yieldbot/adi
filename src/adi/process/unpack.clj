(ns adi.process.unpack
  (:require [hara.common :refer [error hash-map?]]
            [hara.data.map :refer [dissoc-in assoc-in-if]]
            [hara.string.path :as path]
            [hara.data.path :refer [treeify-keys]]
            [datomic.api :as d]))

(declare unpack)

(defn strip-ns [m ns]
  (let [nsv (path/split ns)
        nm  (get-in m nsv)
        xm  (dissoc-in m nsv)]
    (if (empty? xm)
      nm
      (assoc nm :+ xm))))

(defn unpack-ref
  [ent rf attr v adi]
  (let [ns  (-> attr :ref :ns)]
    (cond (nil? rf) nil

          (= v :id)
          (let [k (:ident attr)
                entv (get ent k)]
            (cond (set? entv)
                  (set (map :db/id entv))
                  :else
                  (:db/id entv)))

          (hash-map? v)
          (strip-ns (unpack rf v adi) ns)

          (= v :yield)
          (if (not (@(:seen-ids adi) (:db/id ent)))
            (strip-ns (unpack rf adi) ns)
            {:+ {:db {:id (:db/id ent)}}})

          :else
          (error "RETURN_REF: Cannot process directive: " v))))

(defn wrap-db-id [f]
  (fn [ent model fmodel adi]
    (let [output (f ent model fmodel adi)
          id     (:db/id ent)
          _      (swap! (:seen-ids adi) conj id)]
      (if (-> adi :options :ids)
        (assoc-in output [:db :id] id)
        output))))

(defn wrap-unpack-sets [f]
  (fn [ent attr v adi]
    (let [rf (get ent (-> attr :ref :key))]
      (cond (set? rf)
            (let [res (set (filter identity (map #(f ent % attr v adi) rf)))]
              (if (and (= 1 (count res))
                       (set? (first res)))
                (first res)
                res))

            :else (f ent rf attr v adi)))))

(defn unpack-loop
  ([ent model fmodel adi]
     (unpack-loop ent fmodel fmodel adi {}))
  ([ent model fmodel adi output]
     (if-let [[k v] (first model)]
       (if-let [[attr] (-> adi :schema :flat (get k))]
         (let [noutput (cond (= v :unchecked)
                             output

                             (= :ref (-> attr :type))
                             (assoc-in-if output (path/split k)
                                          ((wrap-unpack-sets unpack-ref) ent attr v adi))

                             (= :enum (-> attr :type))
                             (assoc-in-if output (path/split k)
                                          (if-let [ens (-> attr :enum :ns)]
                                            (-> ent (get k) path/split last)
                                            (get ent k)))

                             (-> adi :options :blank)
                             (if (= v :checked)
                                 (assoc-in-if output (path/split k) (get ent k))
                                 output)

                             :else
                             (assoc-in-if output (path/split k) (get ent k)))]
           (recur ent (next model) fmodel adi noutput))
         (error "RETURN: key " k " is not in schema"))
       output)))

(defn unpack-enums [ent adi]
  (let [ekeys (filter (fn [k] (= :enum (-> adi :schema :flat k first :type)))
                      (keys ent))
        evals (map (fn [k] (let [v (get ent k)
                                ens (-> adi :schema :flat k first :enum :ns)]
                            (if ens
                              (-> (path/split v) last)
                              v)))
                   ekeys)]
    (zipmap ekeys evals)))

(defn unpack
  ([ent adi]
   (if-let [fmodel (-> adi :pipeline :pull)]
     (unpack ent fmodel (assoc adi :seen-ids (atom #{})))
     (let [ent (d/touch ent)
           res (if (-> adi :options :blank)
                 {}
                 (let [ks  (filter (fn [k] (if-let [type (-> adi :schema :flat k first :type)]
                                             (not (#{:enum :alias :ref} type))))
                                   (keys ent))]
                   (-> (select-keys ent ks)
                       (merge (unpack-enums ent adi))
                       (treeify-keys))))]
       (if (-> adi :options :ids)
         (assoc-in res
                   [:db :id] (:db/id ent))
         res))))
  ([ent fmodel adi]
   ((wrap-db-id unpack-loop) ent fmodel fmodel adi)))
