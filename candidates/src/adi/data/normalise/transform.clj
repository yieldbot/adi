(ns adi.data.normalise.transform
  (:require [hara.common :refer [hash-map? hash-set? op]]
            [ribol.core :refer [raise]]))

(defn wrap-hash-set [f]
  (fn [val env]
    (cond (hash-set? val)
          (set (map #(f % env) val))

          :else
          (f val env))))

(defn process-transform [strans tdata nsv interim tsch env]
  (if-let [[k v] (first strans)]
    (cond (and (hash-map? v)
               (-> tsch (get k) vector?)
               (-> tsch (get k) first :type (= :ref)))
          (recur (next strans) tdata nsv interim tsch env)

          :else
          (let [subdata (get tdata k)
                ntdata  (cond (nil? subdata) tdata

                              (fn? v)
                              (assoc tdata k (op (wrap-hash-set v) subdata env))

                              (hash-map? subdata)
                              (assoc tdata k (process-transform
                                              v subdata (conj nsv k)
                                              interim (get tsch k) env))

                              :else
                              (assoc tdata k (if (hash-set? subdata) #{v} v)))]
            (recur (next strans) ntdata nsv interim tsch env)))
    tdata))

(defn wrap-model-pre-transform [f]
  (fn [tdata tsch nsv interim fns env]
    (let [strans (:pre-transform interim)
          output (process-transform strans tdata nsv interim tsch env)]
      (f output tsch nsv (update-in interim [:ref-path]
                                    #(-> %
                                         (pop)
                                         (conj output)))
         fns env))))

(defn wrap-model-post-transform [f]
 (fn [tdata tsch nsv interim fns env]
   (let [strans (:post-transform interim)
         output (f tdata tsch nsv interim fns env)]
     (process-transform strans output nsv interim tsch env))))
