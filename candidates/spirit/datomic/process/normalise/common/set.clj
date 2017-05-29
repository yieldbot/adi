(ns spirit.process.normalise.common.set
  (:require [hara.event :refer [raise]]))

(defn is-query [spirit]
  (= (:type spirit) "query"))

(defn is-set [attr spirit]
  (or (is-query spirit)
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
  (fn [subdata [attr] nsv interim fns spirit]
    (let [subdata
          (cond (and (is-set attr spirit)
                     (not (set? subdata)))
                #{subdata}

                (and (not (is-set attr spirit))
                     (set? subdata))
                (raise [:spirit :normalise :wrong-input {:data subdata :nsv nsv :key-path (:key-path interim)}]
                       (str "WRAP_ATTR_SETS: " subdata " should not be a set"))

                (is-set attr spirit)
                (set subdata)

                :else subdata)]
      (f subdata [attr] nsv interim fns spirit))))
