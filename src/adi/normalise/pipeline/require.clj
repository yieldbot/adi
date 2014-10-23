(ns adi.normalise.pipeline.require
  (:require [hara.common :refer [hash-map?]]
            [ribol.core :refer [raise]]))

(defn process-require [req require-key tdata nsv tsch]
  (if-let [[k v] (first req)]
    (cond (= v :checked)
          (do (if (not (get tdata k))
                (raise [:adi :normalise require-key {:nsv (conj nsv k) :data tdata}]
                       (str "PROCESS_REQUIRE: key " (conj nsv k) " is not present")))
              (recur (next req) require-key tdata nsv tsch))

          (and (-> tsch (get k) vector?)
               (-> tsch (get k) first :type (= :ref)))
          (recur (next req) require-key tdata nsv tsch)

          :else
          (let [subdata (get tdata k)]
            (cond (nil? subdata)
                  (raise [:adi :normalise require-key {:nsv (conj nsv k) :data tdata}]
                         (str "PROCESS_REQUIRE: key " (conj nsv k) " is not present"))

                  (hash-map? subdata)
                  (process-require v require-key (get tdata k) (conj nsv k) (get tsch k)))
            (recur (next req) require-key tdata nsv tsch)))
    tdata))

(defn wrap-model-pre-require [f]
  (fn [tdata tsch nsv interim fns env]
    (let [req (:pre-require interim)]
      (process-require req :no-required tdata nsv tsch)
      (f tdata tsch nsv interim fns env))))

(defn wrap-model-post-require [f]
  (fn [tdata tsch nsv interim fns env]
    (let [req (:post-require interim)
          output (f tdata tsch nsv interim fns env)]
      (process-require req :no-required output nsv tsch))))
