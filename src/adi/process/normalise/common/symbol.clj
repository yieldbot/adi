(ns adi.process.normalise.common.symbol
  (:require [ribol.core :refer [raise]]))
  
(defn wrap-single-symbol
  "wraps normalise to work with symbols for queries as well as :ref attributes of datoms

  (normalise/normalise {:account {:type 'hello}}
                       {:schema (schema/schema {:account/type [{:type :keyword
                                                                :keyword {:ns :account.type}}]})}
                       {:normalise-single [wrap-single-symbol]})
  => {:account {:type '?hello}}"
  {:added "0.3"}
  [f]
  (fn [subdata [attr] nsv interim fns adi]
    (cond (not (symbol? subdata))
          (f subdata [attr] nsv interim fns adi)

          (and (= (:type adi) "datoms")
               (not= (:type attr) :ref))
          (raise [:adi :normalise :ref-only
                  {:nsv nsv :key-path (:key-path interim)}]
                 (str "WRAP_SINGLE_SYMBOL: symbol " subdata " only allowed on refs"))

          (.startsWith (name subdata) "?") subdata

          :else (symbol (str "?" (name subdata))))))