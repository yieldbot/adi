(ns spirit.process.normalise.common.vector
  (:require [hara.event :refer [raise]]
            [hara.common.checks :refer [long?]]
            [clojure.edn :as edn]
            [spirit.data.checks :refer [vexpr?]]
            [spirit.data.common :refer [iid vexpr->expr]]))

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
  (fn [subdata [attr] nsv interim fns spirit]
    (cond (and (vector? subdata)
               (not (vexpr? subdata)))
          (f (set subdata) [attr] nsv interim fns spirit)

          :else
          (f subdata [attr] nsv interim fns spirit))))

(defn wrap-single-vector [f]
  (fn [subdata [attr] nsv interim fns spirit]

    (if (vexpr? subdata)
      (f (vexpr->expr subdata) [attr] nsv interim fns spirit)
      (f subdata [attr] nsv interim fns spirit))))
