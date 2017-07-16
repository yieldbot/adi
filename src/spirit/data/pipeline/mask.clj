(ns spirit.data.pipeline.mask
  (:require [hara.common.checks :refer [hash-map?]]
            [hara.function.args :refer [op]]))

(defn process-mask
  "Used by both wrap-model-pre-mask and wrap-model-post-mask
  for determining correct input
  (pipeline/normalise {:account/name \"Chris\"}
                       {:schema (schema/schema examples/account-name-age-sex)
                        :pipeline {:pre-mask {:account {:name :checked}}}}
                       *wrappers*)
  => {:account {}}

  (pipeline/normalise {:account/age 10}
                       {:schema (schema/schema examples/account-name-age-sex)
                        :pipeline {:pre-mask {:account :checked}}}
                       *wrappers*)
  => {}
  "
  {:added "0.3"}
  [smask tdata nsv interim tsch datasource]
  (if-let [[k v] (first smask)]
    (cond (and (hash-map? v)
               (-> tsch (get k) vector?)
               (-> tsch (get k) first :type (= :ref)))
          (recur (next smask) tdata nsv interim tsch datasource)

          :else
          (let [subdata (get tdata k)]
            (cond (nil? subdata)
                  (recur (next smask) tdata nsv interim tsch datasource)

                  (hash-map? v)
                  (recur (next smask)
                         (assoc tdata k (process-mask v
                                                      subdata
                                                      (conj nsv k)
                                                      interim
                                                      (get tsch k)
                                                      datasource))
                         nsv interim tsch datasource)

                  (= :unchecked v)
                  (recur (next smask) tdata nsv interim tsch datasource)

                  (fn? v)
                  (let [flag (op v subdata datasource)]
                    (if (or (= :unchecked flag)
                            (false? flag)
                            (nil? flag))
                      (recur (next smask) tdata nsv interim tsch datasource)
                      (recur (next smask) (dissoc tdata k) nsv interim tsch datasource)))
                  
                  :else
                  (recur (next smask) (dissoc tdata k) nsv interim tsch datasource))))
    tdata))

(defn wrap-model-pre-mask
  "mask also works across refs
  (pipeline/normalise {:account/orders #{{:number 1 :items {:name \"one\"}}
                                          {:number 2 :items {:name \"two\"}}}}
              {:schema (schema/schema examples/account-orders-items-image)
               :pipeline {:pre-mask {:account {:orders {:number :checked}}}}}
              *wrappers*)
  => {:account {:orders #{{:items {:name \"one\"}}
                          {:items {:name \"two\"}}}}}"
  {:added "0.3"}
  [f]
  (fn [tdata tsch nsv interim fns datasource]
    (let [smask (:pre-mask interim)
          output (process-mask smask tdata nsv interim tsch datasource)]
      (f output tsch nsv (update-in interim [:ref-path]
                                    #(-> %
                                         (pop)
                                         (conj output)))
         fns datasource))))

(defn wrap-model-post-mask [f]
 (fn [tdata tsch nsv interim fns datasource]
   (let [smask (:post-mask interim)
         output (f tdata tsch nsv interim fns datasource)]
     (process-mask smask output nsv interim tsch datasource))))
