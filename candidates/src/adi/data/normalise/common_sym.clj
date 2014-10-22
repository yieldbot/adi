(ns adi.data.normalise.common-sym
  (:require [hara.common :refer [hash-map?]]
            [ribol.core :refer [raise]]
            [adi.common :refer [iid]]))

(defn first-required-key
  ([tsch]
     (first-required-key tsch []))
  ([tsch lvl]
     (if-let [[k v] (first tsch)]
       (cond (hash-map? v)
             (or (first-required-key (get tsch k))
                 (recur (rest tsch) lvl))

             (vector? v)
             (if (-> v first :required)
               (conj lvl k)
               (recur (rest tsch) lvl)))
       (raise [:adi :normalise :needs-require-key]
              (str "FIRST_REQUIRED_KEY: Needs a required key for " lvl)))))

(defn wrap-branch-underscore [f]
  (fn [subdata subsch nsv interim fns env]
    (cond (not (= subdata '_))
          (f subdata subsch nsv interim fns env)

          (= (:type env) "query")
          (assoc-in {} (first-required-key subsch) '#{_})

          :else
          (raise [:adi :normalise :query-only
                  {:nsv nsv :key-path (:key-path interim)}]
                 (str "WRAP_BRANCH_UNDERSCORE: '_' only allowed on queries")))))

(defn wrap-single-symbol [f]
  (fn [subdata [attr] nsv interim fns env]
    (cond (not (symbol? subdata))
          (f subdata [attr] nsv interim fns env)

          (and (= (:type env) "datoms")
               (not= (:type attr) :ref))
          (raise [:adi :normalise :ref-only
                  {:nsv nsv :key-path (:key-path interim)}]
                 (str "WRAP_SINGLE_SYMBOL: symbol " subdata " only allowed on refs"))

          (.startsWith (name subdata) "?") subdata

          :else (symbol (str "?" (name subdata))))))
