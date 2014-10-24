(ns adi.normalise.common.list
  (:require [ribol.core :refer [raise]]))

(defn wrap-single-list [f]
  (fn [subdata [attr] nsv interim fns env]
    (if (list? subdata)
      (cond (-> env :options :ban-expressions)
            (raise [:adi :normalise :not-allowed {:data subdata :nsv nsv :key-path (:key-path interim)}]
               (str "WRAP_SINGLE_LIST: " subdata " not allowed"))

          :else
          ((:normalise-expression fns) subdata [attr] nsv interim env))
      (f subdata [attr] nsv interim fns env))))
