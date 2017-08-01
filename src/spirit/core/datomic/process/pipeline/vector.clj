(ns spirit.core.datomic.process.pipeline.vector
  (:require [hara.event :refer [raise]]
            [hara.common.checks :refer [long?]]
            [clojure.edn :as edn]
            [spirit.core.datomic.data.checks :refer [vexpr?]]
            [spirit.core.datomic.data :refer [iid vexpr->expr]]))

(defn wrap-attr-vector
  "wraps normalise with support for more complex expressions through use of double vector

  (pipeline/normalise {:account {:email [[\":hello\"]]}}
                       {:schema (schema/schema {:account/email [{:type :ref
                                                                :ref {:ns :email}}]})}
                       {:normalise-attr [wrap-attr-vector]
                        :normalise-single [wrap-single-vector]})
  => {:account {:email #db/id[:db.part/user -245025397]}}"
  {:added "0.3"}
  [f]
  (fn [subdata [attr] nsv interim fns datasource]
    (cond (and (vector? subdata)
               (not (vexpr? subdata)))
          (f (set subdata) [attr] nsv interim fns datasource)

          :else
          (f subdata [attr] nsv interim fns datasource))))

(defn wrap-single-vector [f]
  (fn [subdata [attr] nsv interim fns datasource]

    (if (vexpr? subdata)
      (f (vexpr->expr subdata) [attr] nsv interim fns datasource)
      (f subdata [attr] nsv interim fns datasource))))
