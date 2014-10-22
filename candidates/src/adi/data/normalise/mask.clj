(ns adi.data.normalise.mask
  (:require [hara.common :refer [error hash-map? keyword-split]]
            [ribol.core :refer [raise]]))

(defn process-mask [smask tdata nsv interim tsch env]
  (if-let [[k v] (first smask)]
    (cond (and (hash-map? v)
               (-> tsch (get k) vector?)
               (-> tsch (get k) first :type (= :ref)))
          (recur (next smask) tdata nsv interim tsch env)

          :else
          (let [subdata (get tdata k)]
            (cond (nil? subdata)
                  (recur (next smask) tdata nsv interim tsch env)

                  (hash-map? v)
                  (recur (next smask)
                         (assoc tdata k (process-mask v
                                                           subdata
                                                           (conj nsv k)
                                                           interim
                                                           (get tsch k)
                                                           env))
                         nsv interim tsch env)

                  :else
                  (recur (next smask) (dissoc tdata k) nsv interim tsch env))))
    tdata))


(defn wrap-model-pre-mask [f]
  (fn [tdata tsch nsv interim fns env]
    (let [smask (:pre-mask interim)
          output (process-mask smask tdata nsv interim tsch env)]
      (f output tsch nsv (update-in interim [:ref-path]
                                    #(-> %
                                         (pop)
                                         (conj output)))
         fns env))))

(defn wrap-model-post-mask [f]
 (fn [tdata tsch nsv interim fns env]
   (let [smask (:post-mask interim)
         output (f tdata tsch nsv interim fns env)]
     (process-mask smask output nsv interim tsch env))))
