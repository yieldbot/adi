(ns adi.process.normalise.pipeline.fill-assoc
  (:require [hara.common.checks :refer [hash-map?]]
            [hara.data.complex :as complex]
            [hara.function.args :refer [op]]
            [hara.event :refer [raise]]))

(defn process-fill-assoc [sfill tdata nsv interim tsch adi]
  (if-let [[k v] (first sfill)]
    (cond (not (get tdata k))
          (cond (fn? v)
                (recur (next sfill)
                       (assoc tdata k (op v (:ref-path interim) adi))
                       nsv interim tsch adi)

                (hash-map? v)
                (recur (next sfill)
                       (assoc tdata k (process-fill-assoc v
                                                    (get tdata k)
                                                    (conj nsv k)
                                                    interim
                                                    (get tsch k)
                                                    adi))
                       nsv interim tsch adi)

                :else
                (recur (next sfill)
                       (assoc tdata k v) nsv interim tsch adi))

          (and (hash-map? v)
               (-> tsch (get k) vector?)
               (-> tsch (get k) first :type (= :ref)))
          (recur (next sfill) tdata nsv interim tsch adi)

          :else
          (let [subdata (get tdata k)]
            (cond (hash-map? subdata)
                  (recur (next sfill)
                         (assoc tdata k (process-fill-assoc v
                                                      (get tdata k)
                                                      (conj nsv k)
                                                      interim
                                                      (get tsch k)
                                                      adi))
                         nsv interim tsch adi)

                  (fn? v)
                  (recur (next sfill)
                         (complex/assocs tdata k (op v (:ref-path interim) adi))
                         nsv interim tsch adi)

                  :else
                  (recur (next sfill)
                         (complex/assocs tdata k v) nsv interim tsch adi))))
    tdata))

(defn wrap-model-fill-assoc
  "fills data by associating additional elements
  (normalise/normalise {:account/name \"Chris\" :account/age 9}
            {:schema (schema/schema examples/account-name-age-sex)
             :model {:fill-assoc {:account {:age 10}}}}
            *wrappers*)
  => {:account {:name \"Chris\", :age #{9 10}}}

  (normalise/normalise {:account/name \"Chris\"}
            {:schema (schema/schema examples/account-name-age-sex)
             :model {:fill-assoc {:account {:age (fn [_ adi]
                                                   (:age adi))}}}
             :age 10}
            *wrappers*)
  => {:account {:name \"Chris\", :age 10}}
  "
  {:added "0.3"}
  [f]
  (fn [tdata tsch nsv interim fns adi]
    (let [sfill (:fill-assoc interim)]
      (if (hash-map? sfill)
        (let [output (process-fill-assoc sfill tdata nsv interim tsch adi)]
          (f output tsch nsv (update-in interim [:ref-path]
                                        #(-> %
                                             (pop)
                                             (conj output)))
             fns adi))
        (f tdata tsch nsv interim fns adi)))))
