(ns spirit.datomic.process.normalise
  (:require [spirit.datomic.process.normalise.base :as normalise]
            [hara.data.path :as data]
            [hara.common.error :refer [error]]
            [hara.function.args :refer [op]]
            [spirit.datomic.process.normalise.common
              [alias :as alias]
              [db :as db]
              [enum :as enum]
              [keyword :as keyword]
              [list :as list]
              [paths :as paths]
              [set :as set]
              [symbol :as symbol]
              [type-check :as type-check]
              [underscore :as underscore]
              [vector :as vector]]
            [spirit.datomic.process.normalise.pipeline
              [allow :as allow]
              [convert :as convert]
              [expression :as expression]
              [fill-assoc :as fill-assoc]
              [fill-empty :as fill-empty]
              [ignore :as ignore]
              [mask :as mask]
              [require :as require]
              [transform :as transform]
             [validate :as validate]]))


(defn normalise-raw
  ([data datasource]
   (let [tdata (data/treeify-keys-nested data)
         tdata (if-let [pre-process-fn (-> datasource :pipeline :pre-process)]
                 (op pre-process-fn tdata datasource)
                 tdata)
         tsch (-> datasource :schema :tree)
         interim (:pipeline datasource)
         fns {:normalise
              (let [f normalise/normalise-loop
                    f (db/wrap-db f)
                    f (paths/wrap-plus f)
                    f (if (-> datasource :pipeline :fill-assoc)     (fill-assoc/wrap-model-fill-assoc f) f)
                    f (if (-> datasource :pipeline :fill-empty)     (fill-empty/wrap-model-fill-empty f) f)
                    f (if (-> datasource :pipeline :pre-transform)  (transform/wrap-model-pre-transform f) f)
                    f (if (-> datasource :pipeline :pre-mask)       (mask/wrap-model-pre-mask f) f)
                    f (if (-> datasource :pipeline :pre-require)    (require/wrap-model-pre-require f) f)
                    f (if (-> datasource :pipeline :post-require)   (require/wrap-model-post-require f) f)
                    f (if (-> datasource :pipeline :post-mask)      (mask/wrap-model-post-mask f) f)
                    f (if (-> datasource :pipeline :post-transform) (transform/wrap-model-post-transform f) f)
                    f (paths/wrap-ref-path f)
                    f (alias/wrap-alias f)]
                f)

              :normalise-nil
              (let [f normalise/normalise-nil
                    f (if (-> datasource :pipeline :ignore)    (ignore/wrap-nil-model-ignore f) f)]
                f)

              :normalise-branch
              (let [f normalise/normalise-loop
                    f (if (-> datasource :pipeline :allow)    (allow/wrap-branch-model-allow f) f)
                    f (alias/wrap-alias f)
                    f (underscore/wrap-branch-underscore f)
                    f (paths/wrap-key-path f)]
                f)

              :normalise-attr
              (let [f normalise/normalise-attr
                    f (set/wrap-attr-set f)
                    f (vector/wrap-attr-vector f)
                    f (if (-> datasource :pipeline :allow)   (allow/wrap-attr-model-allow f) f)
                    f (paths/wrap-key-path f)]
                f)

              :normalise-expression
              (let [f normalise/normalise-expression
                    f (if (-> datasource :pipeline :expression)  (expression/wrap-single-model-expression f) f)]
                f)

              :normalise-single
              (let [f normalise/normalise-single
                    f (enum/wrap-single-enum f)
                    f (keyword/wrap-single-keyword f)
                    f (if (-> datasource :options :use-typecheck) (type-check/wrap-single-type-check f) f)
                    f (if (-> datasource :pipeline :convert)  (convert/wrap-single-model-convert f) f)
                    f (if (-> datasource :pipeline :validate)  (validate/wrap-single-model-validate f) f)
                    f (list/wrap-single-list f)
                    f (symbol/wrap-single-symbol f)
                    f (vector/wrap-single-vector f)]
                f)}
         output ((:normalise fns) tdata tsch [] interim fns datasource)]
     (if-let [post-process-fn (-> datasource :pipeline :post-process)]
       (post-process-fn output datasource)
       output))))

(defn normalise [datasource]
  (let [data (-> datasource :process :input)
        ndata (if (-> datasource :options :skip-normalise)
                data
                (normalise-raw data datasource))]
    (assoc-in datasource [:process :normalised] ndata)))
