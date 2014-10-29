(ns adi.process.normalise.pipeline.mask
  (:require [hara.common.checks :refer [hash-map?]]))

(defn process-mask
  "Used by both wrap-model-pre-mask and wrap-model-post-mask
  for determining correct input
  (normalise/normalise {:account/name \"Chris\"}
                       {:schema (schema/schema examples/account-name-age-sex)
                        :model {:pre-mask {:account {:name :checked}}}}
                       *wrappers*)
  => {:account {}}

  (normalise/normalise {:account/age 10}
                       {:schema (schema/schema examples/account-name-age-sex)
                        :model {:pre-mask {:account :checked}}}
                       *wrappers*)
  => {}
  "
  {:added "0.3"}
  [smask tdata nsv interim tsch adi]
  (if-let [[k v] (first smask)]
    (cond (and (hash-map? v)
               (-> tsch (get k) vector?)
               (-> tsch (get k) first :type (= :ref)))
          (recur (next smask) tdata nsv interim tsch adi)

          :else
          (let [subdata (get tdata k)]
            (cond (nil? subdata)
                  (recur (next smask) tdata nsv interim tsch adi)

                  (hash-map? v)
                  (recur (next smask)
                         (assoc tdata k (process-mask v
                                                      subdata
                                                      (conj nsv k)
                                                      interim
                                                      (get tsch k)
                                                      adi))
                         nsv interim tsch adi)

                  :else
                  (recur (next smask) (dissoc tdata k) nsv interim tsch adi))))
    tdata))

(defn wrap-model-pre-mask
  "mask also works across refs
  (normalise/normalise {:account/orders #{{:number 1 :items {:name \"one\"}}
                                          {:number 2 :items {:name \"two\"}}}}
              {:schema (schema/schema examples/account-orders-items-image)
               :model {:pre-mask {:account {:orders {:number :checked}}}}}
              *wrappers*)
  => {:account {:orders #{{:items {:name \"one\"}}
                          {:items {:name \"two\"}}}}}"
  {:added "0.3"}
  [f]
  (fn [tdata tsch nsv interim fns adi]
    (let [smask (:pre-mask interim)
          output (process-mask smask tdata nsv interim tsch adi)]
      (f output tsch nsv (update-in interim [:ref-path]
                                    #(-> %
                                         (pop)
                                         (conj output)))
         fns adi))))

(defn wrap-model-post-mask [f]
 (fn [tdata tsch nsv interim fns adi]
   (let [smask (:post-mask interim)
         output (f tdata tsch nsv interim fns adi)]
     (process-mask smask output nsv interim tsch adi))))
