(ns spirit.datomic.process.normalise.pipeline.fill-empty
  (:require [hara.common.checks :refer [hash-map?]]
            [hara.function.args :refer [op]]
            [hara.event :refer [raise]]))

(defn process-fill-empty [sfill tdata nsv interim tsch datasource]
  (if-let [[k v] (first sfill)]
    (cond (not (get tdata k))
          (cond (fn? v)
                (recur (next sfill)
                       (assoc tdata k (op v (:ref-path interim) datasource))
                       nsv interim tsch datasource)

                (hash-map? v)
                (recur (next sfill)
                       (assoc tdata k (process-fill-empty v
                                                    (get tdata k)
                                                    (conj nsv k)
                                                    interim
                                                    (get tsch k)
                                                    datasource))
                       nsv interim tsch datasource)

                :else
                (recur (next sfill)
                       (assoc tdata k v) nsv interim tsch datasource))

          (and (hash-map? v)
               (-> tsch (get k) vector?)
               (-> tsch (get k) first :type (= :ref)))
          (recur (next sfill) tdata nsv interim tsch datasource)

          :else
          (let [subdata (get tdata k)]
            (cond (hash-map? subdata)
                  (recur (next sfill)
                         (assoc tdata k (process-fill-empty v
                                                      (get tdata k)
                                                      (conj nsv k)
                                                      interim
                                                      (get tsch k)
                                                      datasource))
                         nsv interim tsch datasource)
                  :else
                  (recur (next sfill) tdata nsv interim tsch datasource))))
    tdata))

(defn wrap-model-fill-empty
  "fills data by associating additional elements
  (normalise/normalise {:account/name \"Chris\" :account/age 9}
            {:schema (schema/schema examples/account-name-age-sex)
             :pipeline {:fill-empty {:account {:age 10}}}}
            *wrappers*)
  => {:account {:name \"Chris\", :age 9}}

  (normalise/normalise {:account/name \"Chris\"}
            {:schema (schema/schema examples/account-name-age-sex)
             :pipeline {:fill-empty {:account {:age (fn [_ datasource]
                                                   (:age datasource))}}}
             :age 10}
            *wrappers*)
  => {:account {:name \"Chris\", :age 10}}
  "
  {:added "0.3"}
  [f]
  (fn [tdata tsch nsv interim fns datasource]
    (let [sfill (:fill-empty interim)
          output (process-fill-empty sfill tdata nsv interim tsch datasource)]
      (f output tsch nsv (update-in interim [:ref-path]
                                    #(-> %
                                         (pop)
                                         (conj output)))
         fns datasource))))
