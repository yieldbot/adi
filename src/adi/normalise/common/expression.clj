(ns adi.normalise.common.expression
  (:require [ribol.core :refer [raise]]))

(defn wrap-expression-check [f]
  (fn [subdata [attr] nsv interim env]
    (raise [:adi :normalise :not-implemented {:data subdata :nsv nsv :key-path (:key-path interim)}]
               (str "WRAP_EXPRESSION_CHECK: STILL TO BE IMPLEMENTED"))
    (f subdata [attr] nsv interim env)))