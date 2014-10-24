(ns adi.normalise.common.keyword
  (:require [hara.string.path :as path]))

(defn wrap-single-keyword
  "removes the keyword namespace if there is one

  (normalise/normalise {:account {:type :account.type/vip}}
                       {:schema (schema/schema {:account/type [{:type :keyword
                                                                :keyword {:ns :account.type}}]})}
                       {:normalise-single [wrap-single-keyword]})
  => {:account {:type :vip}}"
  {:added "0.3"}
  [f]
  (fn [subdata [attr] nsv interim fns env]
    (cond (= :keyword (:type attr))
          (let [v (if (path/path-ns? subdata (-> attr :keyword :ns))
                    (path/path-stem subdata)
                    subdata)]
            v)
          :else
          (f subdata [attr] nsv interim fns env))))
