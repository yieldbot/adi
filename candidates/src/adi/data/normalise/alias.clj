(ns adi.data.normalise.alias
  (:require [hara.common :refer [hash-map? keyword-split]]
            [hara.collection.data-map :refer [assocs merges-in]]
            [ribol.core :refer [raise]]
            [clojure.walk :as wk]))

(defn find-aliases
  ([tsch ks] (find-aliases tsch ks []))
  ([tsch ks output]
     (if-let [k (first ks)]
       (let [sub (get tsch k)
             noutput (cond (vector? sub)
                       (if-let [alias (-> sub first :alias)]
                         (conj output [k alias])
                         output)
                       :else output)]
         (recur tsch (next ks) noutput))
       output)))

(defn merge-data
  ([tdata adata]
     (merge-data tdata adata (keys adata)))
  ([tdata adata ks]
     (if-let [k (first ks)]
       (let [stdata (get tdata k)
             sadata (get adata k)
             ntdata (cond (and stdata sadata)
                          (assoc tdata k #{stdata sadata})

                          (nil? stdata)
                          (assoc tdata k sadata)

                          (nil? sadata) tdata)]
         (recur ntdata adata (next ks)))
       tdata)))

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
    (wk/postwalk rep-fn tmpl)))

(defn resolve-alias [tsch tdata alias no-gen-sym]
  (let [[k rec] alias
        ans    (keyword-split (:ns rec))
        atmpl  (:template rec)
        atmpl  (if no-gen-sym
                 atmpl
                 (template-alias atmpl))
        sdata  (get tdata k)
        adata  (update-in atmpl ans merge sdata)]
    (merge-data (dissoc tdata k) adata)))

(defn wrap-alias [f]
  (fn [tdata tsch nsv interim fns env]
    (let [ks (keys tdata)
          aliases (find-aliases tsch ks)
          _       (if (and (= :type "datoms")
                           (not (empty? alias)))
                    (raise [:adi :normalise :no-alias
                            {:data tdata :nsv nsv :key-path (:key-path interim)}]
                           (str "WRAP_ALIAS: Aliases cannot be specified on datoms")))
          ntdata (reduce #(resolve-alias tsch %1 %2 (-> env :options :no-alias-gen)) tdata aliases)]
      (f ntdata tsch nsv interim fns env))))
