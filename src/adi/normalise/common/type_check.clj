(ns adi.normalise.common.type-check
  (:require [adi.schema.meta :as meta]
            [adi.data.coerce :refer [coerce]]
            [ribol.core :refer [raise]]))

(defn wrap-single-type-check [f]
  (fn [subdata [attr] nsv interim fns env]
    (let [t (:type attr)
          chk (meta/type-checks t)]
      (cond
       (chk subdata) (f subdata [attr] nsv interim fns env)

       (-> env :options :use-coerce)
       (f (coerce subdata t) [attr] nsv interim fns env)

       :else
       (raise [:adi :normalise :wrong-type
               {:data subdata :nsv nsv :key-path (:key-path interim) :type t}]
               (str "WRAP_SINGLE_TYPE_CHECK: " subdata " in " nsv " is not of type " t))))))
