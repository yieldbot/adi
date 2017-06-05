(ns spirit.common.normalise.base.alias
  (:require [hara.common.checks :refer [hash-map?]]
            [hara.string.path :as path]
            [hara.data.complex :as complex]
            [hara.event :refer [raise]]
            [clojure.walk :as walk]))

(defn find-aliases
  [tsch ks]
  (reduce (fn [out k]
            (let [sub (get tsch k)]
              (cond (vector? sub)
                    (if-let [alias (-> sub first :alias)]
                      (conj out [k alias])
                      out)
                    :else out)))
          []  ks))

(defn template-alias [tmpl]
  (let [symbols (atom {})
        rep-fn (fn [e]
                   (if (symbol? e)
                     (if-let [sym (get @symbols e)]
                       sym
                       (let [sym (gensym (str e "_"))]
                         (swap! symbols assoc e sym)
                         sym))
                     e))]
    (walk/postwalk rep-fn tmpl)))

(defn resolve-alias [tsch tdata alias no-gen-sym]
  (let [[k rec] alias
        ans    (path/split (:ns rec))
        atmpl  (:template rec)
        atmpl  (if no-gen-sym
                 atmpl
                 (template-alias atmpl))
        sdata  (get tdata k)
        adata  (update-in atmpl ans merge sdata)]
    (complex/merges (dissoc tdata k) adata)))

(defn wrap-alias
  "wraps normalise to process aliases for a database schema

  (normalise/normalise {:db/id 'chris
                        :male/name \"Chris\"}
                       {:schema (schema/schema family/family-links)}
                       *wrappers*)
  => '{:db {:id ?chris}, :person {:gender :m, :name \"Chris\"}}

  (normalise/normalise {:female {:parent/name \"Sam\"
                                 :brother {:brother/name \"Chris\"}}}
                       {:schema (schema/schema family/family-links)}
                       *wrappers*)
  => {:person {:gender :f, :parent #{{:name \"Sam\"}},
               :sibling #{{:gender :m, :sibling #{{:name \"Chris\", :gender :m}}}}}}
  "
  {:added "0.3"}
  [f]
  (fn [tdata tsch nsv interim fns datasource]
    (let [ks (keys tdata)
          aliases (find-aliases tsch ks)
          _       (if (and (= (:command datasource) :datoms)
                           (not (empty? aliases)))
                    (raise [:normalise :no-alias
                            {:data tdata :nsv nsv :key-path (:key-path interim)}]
                           (str "WRAP_ALIAS: Aliases cannot be specified on datoms")))
          ntdata (reduce #(resolve-alias tsch %1 %2 (-> datasource :options :no-alias-gen)) tdata aliases)]
      (f ntdata tsch nsv interim fns datasource))))
