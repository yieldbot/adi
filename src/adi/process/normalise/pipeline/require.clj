(ns adi.process.normalise.pipeline.require
  (:require [hara.common :refer [hash-map?]]
            [hara.event :refer [raise]]))

(defn process-require
  "Used by both wrap-model-pre-require and wrap-model-post-require
  for determining correct input
  (normalise/normalise {:account/name \"Chris\"}
                       {:schema (schema/schema examples/account-name-age-sex)
                        :model {:pre-require {:account {:name :checked}}}}
                       *wrappers*)
  => {:account {:name \"Chris\"}}

  (normalise/normalise {:account/age 10}
                       {:schema (schema/schema examples/account-name-age-sex)
                        :model {:pre-require {:account {:name :checked}}}}
                       *wrappers*)
  => (raises-issue {:nsv [:account :name]
                    :no-required true})"
  {:added "0.3"}
  [req require-key tdata nsv tsch]
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

(defn wrap-model-pre-require
  "require also works across refs
  (normalise/normalise {:account/orders #{{:number 1}
                                          {:number 2}}}
              {:schema (schema/schema examples/account-orders-items-image)
               :model {:pre-require {:account {:orders {:number :checked}}}}}
              *wrappers*)
  => {:account {:orders #{{:number 1}
                          {:number 2}}}}
  (normalise/normalise {:account/orders #{{:items {:name \"stuff\"}}
                                          {:number 2}}}
              {:schema (schema/schema examples/account-orders-items-image)
               :model {:pre-require {:account {:orders {:number :checked}}}}}
              *wrappers*)
  => (raises-issue {:data {:items {:name \"stuff\"}}
                    :nsv [:order :number]
                    :no-required true})"
  {:added "0.3"}
  [f]
  (fn [tdata tsch nsv interim fns adi]
    (let [req (:pre-require interim)]
      (process-require req :no-required tdata nsv tsch)
      (f tdata tsch nsv interim fns adi))))

(defn wrap-model-post-require 
  [f]
  (fn [tdata tsch nsv interim fns adi]
    (let [req (:post-require interim)
          output (f tdata tsch nsv interim fns adi)]
      (process-require req :no-required output nsv tsch))))
