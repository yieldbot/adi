(ns spirit.datomic.process.pipeline.set
  (:require [hara.event :refer [raise]]))

(defn is-query [datasource]
  (= (:command datasource) :query))

(defn is-set [attr datasource]
  (or (is-query datasource)
      (= (:cardinality attr) :many)))

(defn wrap-attr-set
  "wraps normalise to type check inputs as well as to coerce incorrect inputs
  (pipeline/normalise {:account {:tags \"10\"}}
                       {:schema (schema/schema examples/account-orders-items-image)}
                       {:normalise-attr [wrap-attr-set]})
  => {:account {:tags #{\"10\"}}}

  (pipeline/normalise {:account {:user #{\"andy\" \"bob\"}}}
                       {:schema (schema/schema examples/account-orders-items-image)
                        :type \"query\"}
                       {:normalise-attr [wrap-attr-set]})
  => {:account {:user #{\"bob\" \"andy\"}}}"
  {:added "0.3"}
  [f]
  (fn [subdata [attr] nsv interim fns datasource]
    (let [subdata
          (cond (and (is-set attr datasource)
                     (not (set? subdata)))
                #{subdata}

                (and (not (is-set attr datasource))
                     (set? subdata))
                (raise [:normalise :wrong-input {:data subdata :nsv nsv :key-path (:key-path interim)}]
                       (str "WRAP_ATTR_SETS: " subdata " should not be a set"))

                (is-set attr datasource)
                (set subdata)

                :else subdata)]
      (f subdata [attr] nsv interim fns datasource))))
