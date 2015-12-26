(ns adi.process.normalise.pipeline.transform
  (:require [hara.common :refer [hash-map?]]
            [hara.event :refer [raise]]
            [hara.function.args :refer [op]]))

(defn wrap-hash-set [f]
  (fn [val adi]
    (cond (set? val)
          (set (map #(op f % adi) val))

          :else
          (op f val adi))))

(defn process-transform
  "Used by both wrap-model-pre-transform and wrap-model-post-transform
  for determining correct input

  (normalise/normalise {:account/name \"Chris\"}
                       {:schema (schema/schema examples/account-name-age-sex)
                        :model {:pre-transform {:account {:name \"Bob\"}}}}
                       *wrappers*)
  => {:account {:name \"Bob\"}}

  (normalise/normalise {:account/name \"Chris\"}
                       {:schema (schema/schema examples/account-name-age-sex)
                        :name \"Bob\"
                        :model {:pre-transform {:account {:name (fn [_ adi] (:name adi))}}}}
                       *wrappers*)
  => {:account {:name \"Bob\"}}

  (normalise/normalise {:account/name \"Chris\"}
                       {:schema (schema/schema examples/account-name-age-sex)
                        :name \"Bob\"
                        :model {:pre-transform {:account {:name (fn [v] (str v \"tian\"))}}}}
                       *wrappers*)
  => {:account {:name \"Christian\"}}"
  {:added "0.3"}
  [strans tdata nsv interim tsch adi]
  (if-let [[k v] (first strans)]
    (cond (and (hash-map? v)
               (-> tsch (get k) vector?)
               (-> tsch (get k) first :type (= :ref)))
          (recur (next strans) tdata nsv interim tsch adi)

          :else
          (let [subdata (get tdata k)
                ntdata  (cond (nil? subdata) tdata

                              (fn? v)
                              (assoc tdata k ((wrap-hash-set v) subdata adi))

                              (hash-map? subdata)
                              (assoc tdata k (process-transform
                                              v subdata (conj nsv k)
                                              interim (get tsch k) adi))

                              :else
                              (assoc tdata k (if (set? subdata) #{v} v)))]
            (recur (next strans) ntdata nsv interim tsch adi)))
    tdata))

(defn wrap-model-pre-transform
  "transform also works across refs
  (normalise/normalise {:account/orders #{{:number 1 :items {:name \"one\"}}
                                          {:number 2 :items {:name \"two\"}}}}
              {:schema (schema/schema examples/account-orders-items-image)
               :model {:pre-transform {:account {:orders {:number inc
                                                          :items {:name \"thing\"}}}}}}
              *wrappers*)
  => {:account {:orders #{{:items {:name \"thing\"}, :number 2}
                          {:items {:name \"thing\"}, :number 3}}}}"
  {:added "0.3"}
  [f]
  (fn [tdata tsch nsv interim fns adi]
    (let [strans (:pre-transform interim)
          output (process-transform strans tdata nsv interim tsch adi)]
      (f output tsch nsv (update-in interim [:ref-path]
                                    #(-> %
                                         (pop)
                                         (conj output)))
         fns adi))))

(defn wrap-model-post-transform [f]
 (fn [tdata tsch nsv interim fns adi]
   (let [strans (:post-transform interim)
         output (f tdata tsch nsv interim fns adi)]
     (process-transform strans output nsv interim tsch adi))))
