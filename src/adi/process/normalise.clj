(ns adi.process.normalise
  (:require [adi.process.normalise.base :as normalise]
            [hara.data.path :as data]
            [hara.common.error :refer [error]]
            [adi.process.normalise.common
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
            [adi.process.normalise.pipeline
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
  ([data adi]
   (let [tdata (data/treeify-keys-nested data)
         tdata (if-let [pre-process-fn (-> adi :pipeline :pre-process)]
                 (pre-process-fn tdata adi)
                 tdata)
         tsch (-> adi :schema :tree)
         interim (:pipeline adi)
         fns {:normalise
              (let [f normalise/normalise-loop
                    f (db/wrap-db f)
                    f (paths/wrap-plus f)
                    f (if (-> adi :pipeline :fill-assoc)     (fill-assoc/wrap-model-fill-assoc f) f)
                    f (if (-> adi :pipeline :fill-empty)     (fill-empty/wrap-model-fill-empty f) f)
                    f (if (-> adi :pipeline :pre-transform)  (transform/wrap-model-pre-transform f) f)
                    f (if (-> adi :pipeline :pre-mask)       (mask/wrap-model-pre-mask f) f)
                    f (if (-> adi :pipeline :pre-require)    (require/wrap-model-pre-require f) f)
                    f (if (-> adi :pipeline :post-require)   (require/wrap-model-post-require f) f)
                    f (if (-> adi :pipeline :post-mask)      (mask/wrap-model-post-mask f) f)
                    f (if (-> adi :pipeline :post-transform) (transform/wrap-model-post-transform f) f)
                    f (paths/wrap-ref-path f)
                    f (alias/wrap-alias f)]
                f)

              :normalise-nil
              (let [f normalise/normalise-nil
                    f (if (-> adi :pipeline :ignore)    (ignore/wrap-nil-model-ignore f) f)]
                f)

              :normalise-branch
              (let [f normalise/normalise-loop
                    f (if (-> adi :pipeline :allow)    (allow/wrap-branch-model-allow f) f)
                    f (alias/wrap-alias f)
                    f (underscore/wrap-branch-underscore f)
                    f (paths/wrap-key-path f)]
                f)

              :normalise-attr
              (let [f normalise/normalise-attr
                    f (set/wrap-attr-set f)
                    f (vector/wrap-attr-vector f)
                    f (if (-> adi :pipeline :allow)   (allow/wrap-attr-model-allow f) f)
                    f (paths/wrap-key-path f)]
                f)

              :normalise-expression
              (let [f normalise/normalise-expression
                    f (if (-> adi :pipeline :expression)  (expression/wrap-single-model-expression f) f)]
                f)

              :normalise-single
              (let [f normalise/normalise-single
                    f (enum/wrap-single-enum f)
                    f (keyword/wrap-single-keyword f)
                    f (if (-> adi :options :use-typecheck) (type-check/wrap-single-type-check f) f)
                    f (if (-> adi :pipeline :convert)  (convert/wrap-single-model-convert f) f)
                    f (if (-> adi :pipeline :validate)  (validate/wrap-single-model-validate f) f)
                    f (list/wrap-single-list f)
                    f (symbol/wrap-single-symbol f)
                    f (vector/wrap-single-vector f)]
                f)}
         output ((:normalise fns) tdata tsch [] interim fns adi)]
     (if-let [post-process-fn (-> adi :pipeline :post-process)]
       (post-process-fn output adi)
       output))))

(defn normalise [adi]
  (let [data (-> adi :process :input)
        ndata (if (-> adi :options :skip-normalise)
                data
                (normalise-raw data adi))]
    (assoc-in adi [:process :normalised] ndata)))
