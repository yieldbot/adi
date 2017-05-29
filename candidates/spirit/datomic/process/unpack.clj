(ns spirit.process.unpack
  (:require [hara.common
             [checks :refer [hash-map?]]
             [error :refer [error]]]
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
  [ent rf attr v spirit]
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
          (strip-ns (unpack rf v spirit) ns)

          (= v :yield)
          (if (not (@(:seen-ids spirit) (:db/id ent)))
            (strip-ns (unpack rf spirit) ns)
            {:+ {:db {:id (:db/id ent)}}})

          :else
          (error "RETURN_REF: Cannot process directive: " v))))

(defn wrap-db-id [f]
  (fn [ent model fmodel spirit]
    (let [output (f ent model fmodel spirit)
          id     (:db/id ent)
          _      (swap! (:seen-ids spirit) conj id)]
      (if (-> spirit :options :ids)
        (assoc-in output [:db :id] id)
        output))))

(defn wrap-unpack-sets [f]
  (fn [ent attr v spirit]
    (let [rf (get ent (-> attr :ref :key))]
      (cond (set? rf)
            (let [res (set (filter identity (map #(f ent % attr v spirit) rf)))]
              (if (and (= 1 (count res))
                       (set? (first res)))
                (first res)
                res))

            :else (f ent rf attr v spirit)))))

(defn unpack-loop
  ([ent model fmodel spirit]
     (unpack-loop ent fmodel fmodel spirit {}))
  ([ent model fmodel spirit output]
     (if-let [[k v] (first model)]
       (if-let [[attr] (-> spirit :schema :flat (get k))]
         (let [noutput (cond (= v :unchecked)
                             output

                             (= :ref (-> attr :type))
                             (assoc-in-if output (path/split k)
                                          ((wrap-unpack-sets unpack-ref) ent attr v spirit))

                             (= :enum (-> attr :type))
                             (assoc-in-if output (path/split k)
                                          (if-let [ens (-> attr :enum :ns)]
                                            (-> ent (get k) path/split last)
                                            (get ent k)))

                             (-> spirit :options :blank)
                             (if (= v :checked)
                                 (assoc-in-if output (path/split k) (get ent k))
                                 output)

                             :else
                             (assoc-in-if output (path/split k) (get ent k)))]
           (recur ent (next model) fmodel spirit noutput))
         (error "RETURN: key " k " is not in schema"))
       output)))

(defn unpack-enums [ent spirit]
  (let [ekeys (filter (fn [k] (= :enum (-> spirit :schema :flat k first :type)))
                      (keys ent))
        evals (map (fn [k] (let [v (get ent k)
                                ens (-> spirit :schema :flat k first :enum :ns)]
                            (if ens
                              (-> (path/split v) last)
                              v)))
                   ekeys)]
    (zipmap ekeys evals)))

(defn unpack
  ([ent spirit]
   (if-let [fmodel (-> spirit :pipeline :pull)]
     (unpack ent fmodel (assoc spirit :seen-ids (atom #{})))
     (let [ent (d/touch ent)
           res (if (-> spirit :options :blank)
                 {}
                 (let [ks  (filter (fn [k] (if-let [type (-> spirit :schema :flat k first :type)]
                                             (not (#{:enum :alias :ref} type))))
                                   (keys ent))]
                   (-> (select-keys ent ks)
                       (merge (unpack-enums ent spirit))
                       (treeify-keys))))]
       (if (-> spirit :options :ids)
         (assoc-in res
                   [:db :id] (:db/id ent))
         res))))
  ([ent fmodel spirit]
   ((wrap-db-id unpack-loop) ent fmodel fmodel spirit)))
