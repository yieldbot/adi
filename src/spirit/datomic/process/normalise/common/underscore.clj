(ns spirit.datomic.process.normalise.common.underscore
  (:require [hara.common.checks :refer [hash-map?]]
            [hara.event :refer [raise]]))

(defn rep-key
  "finds the :required or :representative key within a schema,
  otherwise throws an error
  (rep-key (:account examples/account-orders-items-image))
  => [:user]

  (rep-key (:order examples/account-orders-items-image))
  => (raises-issue {:needs-require-key true})"
  {:added "0.3"}
  ([tsch]
     (rep-key tsch []))
  ([tsch lvl]
     (if-let [[k v] (first tsch)]
       (cond (hash-map? v)
             (or (rep-key (get tsch k))
                 (recur (rest tsch) lvl))

             (vector? v)
             (if (or (-> v first :required)
                     (-> v first :representative))
               (conj lvl k)
               (recur (rest tsch) lvl)))
       (raise [:normalise :needs-require-key]
              (str "REP_KEY: Needs a :required or :representative key for " lvl)))))

(defn wrap-branch-underscore
  "wraps normalise to process underscores
  (normalise/normalise {:account '_}
                       {:schema (schema/schema examples/account-orders-items-image)
                        :type \"query\"}
                       {:normalise-branch [wrap-branch-underscore]})
  => {:account {:user '#{_}}}"
  {:added "0.3"}
  [f]
  (fn [subdata subsch nsv interim fns datasource]
    (cond (not (= subdata '_))
          (f subdata subsch nsv interim fns datasource)

          (= (:command datasource) :query)
          (assoc-in {} (rep-key subsch) '#{_})

          :else
          (raise [:normalise :query-only
                  {:nsv nsv :key-path (:key-path interim)}]
                 (str "WRAP_BRANCH_UNDERSCORE: '_' only allowed on queries")))))
