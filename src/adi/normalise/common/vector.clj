(ns adi.normalise.common.vector
  (:require [ribol.core :refer [raise]]
            [clojure.edn :as edn]
            [adi.data.common :refer [iid]]))

(defn is-vecxpr [v]
  (and (vector? v)
       (= 1 (count v))
       (vector? (first v))
       (let [[[s]] v] (string? s))))

(defn vecxpr->xpr [v]
  (let [[[s]] v]
    (let [xpr (edn/read-string s)]
      (cond
       (= '_ xpr) '_

       (list? xpr) xpr

       (and (symbol? xpr) (.startsWith s "?")) xpr

       (keyword? xpr) (iid xpr)

       :else
       (raise [:adi :normalise :wrong-input {:data v}]
              (str "VECXPR->XPR: wrong input given in vector expression: " v))))))

(defn wrap-attr-vector [f]
  (fn [subdata [attr] nsv interim fns env]
    (cond (and (vector? subdata)
               (not (is-vecxpr subdata)))
          (f (set subdata) [attr] nsv interim fns env)

          :else
          (f subdata [attr] nsv interim fns env))))

(defn wrap-single-vector [f]
  (fn [subdata [attr] nsv interim fns env]
    (if (vector? subdata)
      (if (is-vecxpr subdata)
        (f (vecxpr->xpr subdata) [attr] nsv interim fns env)
        (raise [:adi :normalise :wrong-input {:data subdata :nsv nsv :key-path (:key-path interim)}]
               (str "WRAP_SINGLE_VECTOR: " subdata " should be a vector expression")))
      (f subdata [attr] nsv interim fns env))))
