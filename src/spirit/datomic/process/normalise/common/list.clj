(ns spirit.datomic.process.normalise.common.list
  (:require [hara.event :refer [raise]]))

(defn wrap-single-list
  "wraps normalise with support for more complex expressions through use of double vector

  (normalise/normalise {:account {:age '(< ? 1)}}
                       {:schema (schema/schema {:account/age [{:type :long}]})}
                       {:normalise-single [wrap-single-list]})
  => {:account {:age '(< ? 1)}}"
  {:added "0.3"}
  [f]
  (fn [subdata [attr] nsv interim fns datasource]
    (if (list? subdata)
      (cond (-> datasource :options :ban-expressions)
            (raise [:normalise :not-allowed {:data subdata :nsv nsv :key-path (:key-path interim)
                                                  :options (:options datasource)}]
               (str "WRAP_SINGLE_LIST: " subdata " not allowed"))

          :else
          ((:normalise-expression fns) subdata [attr] nsv interim datasource))
      (f subdata [attr] nsv interim fns datasource))))
