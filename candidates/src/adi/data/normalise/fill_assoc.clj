(ns adi.data.normalise.fill-assoc
  (:require [hara.common :refer [hash-map? op]]
            [hara.collection.data-map :refer [assocs]]
            [ribol.core :refer [raise]]))

(defn process-fill-assoc [sfill tdata nsv interim tsch env]
  (if-let [[k v] (first sfill)]
    (cond (not (get tdata k))
          (cond (fn? v)
                (recur (next sfill)
                       (assoc tdata k (op v (:ref-path interim) env))
                       nsv interim tsch env)

                (hash-map? v)
                (recur (next sfill)
                       (assoc tdata k (process-fill-assoc v
                                                    (get tdata k)
                                                    (conj nsv k)
                                                    interim
                                                    (get tsch k)
                                                    env))
                       nsv interim tsch env)

                :else
                (recur (next sfill)
                       (assoc tdata k v) nsv interim tsch env))

          (and (hash-map? v)
               (-> tsch (get k) vector?)
               (-> tsch (get k) first :type (= :ref)))
          (recur (next sfill) tdata nsv interim tsch env)

          :else
          (let [subdata (get tdata k)]
            (cond (hash-map? subdata)
                  (recur (next sfill)
                         (assoc tdata k (process-fill-assoc v
                                                      (get tdata k)
                                                      (conj nsv k)
                                                      interim
                                                      (get tsch k)
                                                      env))
                         nsv interim tsch env)

                  (fn? v)
                  (recur (next sfill)
                         (assocs tdata k (op v (:ref-path interim) env))
                         nsv interim tsch env)

                  :else
                  (recur (next sfill)
                         (assocs tdata k v) nsv interim tsch env))))
    tdata))

(defn wrap-model-fill-assoc [f]
  (fn [tdata tsch nsv interim fns env]
    (let [sfill (:fill-assoc interim)]
      (if (hash-map? sfill)
        (let [output (process-fill-assoc sfill tdata nsv interim tsch env)]
          (f output tsch nsv (update-in interim [:ref-path]
                                        #(-> %
                                             (pop)
                                             (conj output)))
             fns env))
        (f tdata tsch nsv interim fns env)))))
