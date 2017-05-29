(ns spirit.process.normalise.pipeline.mask
  (:require [hara.common.checks :refer [hash-map?]]
            [hara.function.args :refer [op]]))

(defn process-mask
  "Used by both wrap-model-pre-mask and wrap-model-post-mask
  for determining correct input
  (normalise/normalise {:account/name \"Chris\"}
                       {:schema (schema/schema examples/account-name-age-sex)
                        :pipeline {:pre-mask {:account {:name :checked}}}}
                       *wrappers*)
  => {:account {}}

  (normalise/normalise {:account/age 10}
                       {:schema (schema/schema examples/account-name-age-sex)
                        :pipeline {:pre-mask {:account :checked}}}
                       *wrappers*)
  => {}
  "
  {:added "0.3"}
  [smask tdata nsv interim tsch spirit]
  (if-let [[k v] (first smask)]
    (cond (and (hash-map? v)
               (-> tsch (get k) vector?)
               (-> tsch (get k) first :type (= :ref)))
          (recur (next smask) tdata nsv interim tsch spirit)

          :else
          (let [subdata (get tdata k)]
            (cond (nil? subdata)
                  (recur (next smask) tdata nsv interim tsch spirit)

                  (hash-map? v)
                  (recur (next smask)
                         (assoc tdata k (process-mask v
                                                      subdata
                                                      (conj nsv k)
                                                      interim
                                                      (get tsch k)
                                                      spirit))
                         nsv interim tsch spirit)

                  (= :unchecked v)
                  (recur (next smask) tdata nsv interim tsch spirit)

                  (fn? v)
                  (let [flag (op v subdata spirit)]
                    (if (or (= :unchecked flag)
                            (false? flag)
                            (nil? flag))
                      (recur (next smask) tdata nsv interim tsch spirit)
                      (recur (next smask) (dissoc tdata k) nsv interim tsch spirit)))
                  
                  :else
                  (recur (next smask) (dissoc tdata k) nsv interim tsch spirit))))
    tdata))

(defn wrap-model-pre-mask
  "mask also works across refs
  (normalise/normalise {:account/orders #{{:number 1 :items {:name \"one\"}}
                                          {:number 2 :items {:name \"two\"}}}}
              {:schema (schema/schema examples/account-orders-items-image)
               :pipeline {:pre-mask {:account {:orders {:number :checked}}}}}
              *wrappers*)
  => {:account {:orders #{{:items {:name \"one\"}}
                          {:items {:name \"two\"}}}}}"
  {:added "0.3"}
  [f]
  (fn [tdata tsch nsv interim fns spirit]
    (let [smask (:pre-mask interim)
          output (process-mask smask tdata nsv interim tsch spirit)]
      (f output tsch nsv (update-in interim [:ref-path]
                                    #(-> %
                                         (pop)
                                         (conj output)))
         fns spirit))))

(defn wrap-model-post-mask [f]
 (fn [tdata tsch nsv interim fns spirit]
   (let [smask (:post-mask interim)
         output (f tdata tsch nsv interim fns spirit)]
     (process-mask smask output nsv interim tsch spirit))))
