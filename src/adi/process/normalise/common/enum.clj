(ns adi.process.normalise.common.enum
  (:require [ribol.core :refer [raise]]
            [hara.common.error :as error]
            [hara.string.path :as path]))

(defn wrap-single-enum
  "wraps normalise with comprehension of the enum type

  (normalise/normalise {:account {:type :account.type/guest}}
                       {:schema (schema/schema {:account/type [{:type :enum
                                                                :enum {:ns :account.type
                                                                       :values #{:vip :guest}}}]})}
                       {:normalise-single [wrap-single-enum]})
  => {:account {:type :guest}}
  "
  {:added "0.3"}
  [f]
  (fn [subdata [attr] nsv interim fns env]
    (cond (= :enum (:type attr))
          (let [v (if (path/path-ns? subdata (-> attr :enum :ns))
                    (path/path-stem subdata)
                    subdata)
                chk (-> attr :enum :values)]
            (if-not (error/suppress (chk v))
              (raise [:adi :normalise :wrong-input
                {:data subdata :nsv nsv :key-path (:key-path interim) :check chk}]
                (str "WRAP_SINGLE_ENUMS: " v " in " nsv " can only be one of: " chk))
              (f v [attr] nsv interim fns env)))
          :else
          (f subdata [attr] nsv interim fns env))))
