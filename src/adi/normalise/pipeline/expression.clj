(ns adi.normalise.pipeline.expression
  (:require [hara.common.error :refer [error suppress]]
            [hara.common.checks :refer [hash-map?]]
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

        (set? chk)
        (some #(check-expr % v) chk)

        :else
        (error "CHECK_EXPR: checker " chk " is not in the correct format")))

(defn wrap-single-model-expression
  "controls the expressions allowed for searches
   (normalise/normalise {:account/name '(= \"Chris\")}
                {:schema (schema/schema examples/account-name-age-sex)
                 :model {:expression {:account {:name '(= \"Chris\")}}}}
                *wrappers*)
   => {:account {:name '(= \"Chris\")}}

   (normalise/normalise {:account/name '(= \"Chris\")}
                {:schema (schema/schema examples/account-name-age-sex)
                 :model {:expression {:account {:name '#{=}}}}}
                *wrappers*)
   => {:account {:name '(= \"Chris\")}}
   "
  {:added "0.3"}
  [f]
  (fn [subdata [attr] nsv interim env]
    (let [subexpression (:expression interim)]
      (cond (hash-map? subexpression)
            (f subdata [attr] nsv interim env)

            (and (not (nil? subexpression))
                 (check-expr subexpression subdata)) subdata

            :else
            (raise [:adi :normalise :expression :failed-check
                    {:data subdata :nsv nsv :key-path (:key-path interim)}]
                    (str "WRAP_SINGLE_MODEL_EXPRESSION: " (:key-path interim) " failed check."))))))
