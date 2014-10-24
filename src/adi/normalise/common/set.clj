(ns adi.normalise.common.set
  (:require [ribol.core :refer [raise]]))

(defn is-query [env]
  (= (:type env) "query"))

(defn is-set [attr env]
  (or (is-query env)
      (= (:cardinality attr) :many)))
  
(defn wrap-attr-sets [f]
  (fn [subdata [attr] nsv interim fns env]
    (let [subdata
          (cond (and (is-set attr env)
                     (not (set? subdata)))
                #{subdata}

                (and (not (is-set attr env))
                     (set? subdata))
                (raise [:adi :normalise :wrong-input {:data subdata :nsv nsv :key-path (:key-path interim)}]
                       (str "WRAP_ATTR_SETS: " subdata " should not be a set"))

                (is-set attr env)
                (set subdata)

                :else subdata)]
      (f subdata [attr] nsv interim fns env))))