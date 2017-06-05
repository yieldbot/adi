(ns spirit.common.pipeline.base.keyword
  (:require [hara.string.path :as path]))

(defn wrap-single-keyword
  "removes the keyword namespace if there is one

  (pipeline/normalise {:account {:type :account.type/vip}}
                       {:schema (schema/schema {:account/type [{:type :keyword
                                                                :keyword {:ns :account.type}}]})}
                       {:normalise-single [wrap-single-keyword]})
  => {:account {:type :vip}}"
  {:added "0.3"}
  [f]
  (fn [subdata [attr] nsv interim fns datasource]
    (cond (= :keyword (:type attr))
          (let [kns (-> attr :keyword :ns)
                v   (if (and kns (path/path-ns? subdata kns))
                      (path/path-stem subdata)
                      subdata)]
            v)
          :else
          (f subdata [attr] nsv interim fns datasource))))
