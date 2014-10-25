(ns adi.data.normalise.expressions
  (:require [hara.common :refer [error suppress hash-set?]]
            [ribol.core :refer [raise]]))

(defn chk-expr-sym [chk-sym v-sym]
  (cond (= chk-sym '_) true
        (fn? chk-sym) (suppress (chk-sym v-sym))
        (symbol? chk-sym) (if (= chk-sym v-sym) true
                              (when-let [chk (suppress (resolve chk-sym))]
                                (suppress (chk v-sym))))
        :else (= chk-sym v-sym)))

(defn check-expr [chk v]
  (cond (= chk '_) true

        (symbol? chk)
        (= (first v) chk)

        (list? chk)
        (and (= (count chk) (count v))
             (every? identity (map chk-expr-sym chk v)))

        (fn? chk) (suppress (chk v))

        (vector? chk)
        (every? #(check-expr % v) chk)

        (hash-set? chk)
        (some #(check-expr % v) chk)

        :else
        (error "CHECK_EXPR: checker " chk " is not in the correct format")))

(defn wrap-single-model-expressions [f]
  (fn [subdata [attr] nsv interim env]
    (let [subexpression (:expressions interim)]
      (cond (and (not (nil? subexpression))
                 (check-expr subexpression subdata)) subdata

            :else
            (raise [:adi :normalise :expression :failed-check
                    {:data subdata :nsv nsv :key-path (:key-path interim)}]
                    (str "WRAP_SINGLE_MODEL_EXPRESSION: " (:key-path interim) " failed check."))))))
