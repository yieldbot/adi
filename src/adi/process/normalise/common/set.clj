(ns adi.process.normalise.common.set
  (:require [ribol.core :refer [raise]]))

(defn is-query [env]
  (= (:type env) "query"))

(defn is-set [attr env]
  (or (is-query env)
      (= (:cardinality attr) :many)))
  
(defn wrap-attr-set
  "wraps normalise to type check inputs as well as to coerce incorrect inputs
  (normalise/normalise {:account {:tags \"10\"}}
                       {:schema (schema/schema examples/account-orders-items-image)}
                       {:normalise-attr [wrap-attr-set]})
  => {:account {:tags #{\"10\"}}}

  (normalise/normalise {:account {:user #{\"andy\" \"bob\"}}}
                       {:schema (schema/schema examples/account-orders-items-image)
                        :type \"query\"}
                       {:normalise-attr [wrap-attr-set]})
  => {:account {:user #{\"bob\" \"andy\"}}}"
  {:added "0.3"}
  [f]
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