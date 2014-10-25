(ns adi.data.normalise.common-col
  (:require [hara.common :refer [hash-map? hash-set? long? assoc-if
                                 keyword-join keyword-split
                                 keyword-ns keyword-ns? keyword-stem]]
            [adi.common :refer [iid]]
            [hara.collection.hash-map :refer [treeify-keys]]
            [clojure.edn :as edn]
            [ribol.core :refer [raise]]))

(defn is-query [env]
  (= (:type env) "query"))

(defn is-set [attr env]
  (or (is-query env)
      (= (:cardinality attr) :many)))

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

(defn wrap-attr-sets [f]
  (fn [subdata [attr] nsv interim fns env]
    (let [subdata
          (cond (and (is-set attr env)
                     (not (hash-set? subdata)))
                #{subdata}

                (and (not (is-set attr env))
                     (hash-set? subdata))
                (raise [:adi :normalise :wrong-input {:data subdata :nsv nsv :key-path (:key-path interim)}]
                       (str "WRAP_ATTR_SETS: " subdata " should not be a set"))

                (is-set attr env)
                (set subdata)

                :else subdata)]
      (f subdata [attr] nsv interim fns env))))

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

(defn wrap-single-list [f]
  (fn [subdata [attr] nsv interim fns env]
    (if (list? subdata)
      (cond (-> env :options :ban-expressions)
            (raise [:adi :normalise :not-allowed {:data subdata :nsv nsv :key-path (:key-path interim)}]
               (str "WRAP_SINGLE_LIST: " subdata " not allowed"))

          :else
          ((:normalise-expression fns) subdata [attr] nsv interim env))
      (f subdata [attr] nsv interim fns env))))

(defn wrap-expression-check [f]
  (fn [subdata [attr] nsv interim env]
    (raise [:adi :normalise :not-implemented {:data subdata :nsv nsv :key-path (:key-path interim)}]
               (str "WRAP_EXPRESSION_CHECK: STILL TO BE IMPLEMENTED"))
    (f subdata [attr] nsv interim env)))
