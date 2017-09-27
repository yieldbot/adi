(ns adi.process.normalise.common.set
  (:require [hara.event :refer [raise]]))

(defn is-query [adi]
  (= (:type adi) "query"))

(defn is-set [attr adi]
  (or (is-query adi)
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
  (fn [subdata [attr] nsv interim fns adi]
    (let [subdata
          (cond (and (is-set attr adi)
                     (not (set? subdata)))
                #{subdata}

                (and (not (is-set attr adi))
                     (set? subdata))
                (raise [:adi :normalise :wrong-input {:data subdata :nsv nsv :key-path (:key-path interim)}]
                       (str "WRAP_ATTR_SETS: " subdata " should not be a set"))

                (is-set attr adi)
                (set subdata)

                :else subdata)]
      (f subdata [attr] nsv interim fns adi))))
