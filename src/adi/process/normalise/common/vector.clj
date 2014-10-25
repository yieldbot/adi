(ns adi.process.normalise.common.vector
  (:require [ribol.core :refer [raise]]
            [clojure.edn :as edn]
            [adi.data.common :refer [iid]]))

(defn is-vecxpr
  "checks whether an input is a vector expression
  (is-vecxpr [[\":hello\"]]) => true"
  {:added "0.3"}
  [v]
  (and (vector? v)
       (= 1 (count v))
       (vector? (first v))
       (let [[[s]] v] (string? s))))

(defn vecxpr->xpr
  "checks whether an input is a vector expression
  (vecxpr->xpr [[\"_\"]]) => '_

  (vecxpr->xpr [[\"?hello\"]]) => '?hello

  (vecxpr->xpr [[\"(< ? 1)\"]]) => '(< ? 1)

  (vecxpr->xpr [[\":hello\"]]) => #db/id[:db.part/user -245025397]"
  {:added "0.3"}
  [v]
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
