(ns adi.process.normalise.common.vector
  (:require [ribol.core :refer [raise]]
            [hara.common.checks :refer [long?]]
            [clojure.edn :as edn]
            [adi.data.checks :refer [vexpr?]]
            [adi.data.common :refer [iid vexpr->expr]]))

(defn wrap-attr-vector
  "wraps normalise with support for more complex expressions through use of double vector

  (normalise/normalise {:account {:email [[\":hello\"]]}}
                       {:schema (schema/schema {:account/email [{:type :ref
                                                                :ref {:ns :email}}]})}
                       {:normalise-attr [wrap-attr-vector]
                        :normalise-single [wrap-single-vector]})
  => {:account {:email #db/id[:db.part/user -245025397]}}"
  {:added "0.3"}
  [f]
  (fn [subdata [attr] nsv interim fns env]
    (cond (and (vector? subdata)
               (not (vexpr? subdata)))
          (f (set subdata) [attr] nsv interim fns env)

          :else
          (f subdata [attr] nsv interim fns env))))

(defn wrap-single-vector [f]
  (fn [subdata [attr] nsv interim fns env]

    (if (vexpr? subdata)
      (f (vexpr->expr subdata) [attr] nsv interim fns env)
      (f subdata [attr] nsv interim fns env))))
