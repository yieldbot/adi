(ns adi.normalise
  (:require [adi.normalise.base :as normalise]
            [hara.data.path :as data]
            #_[adi.data.normalise.alias :refer [wrap-alias]]
            #_[adi.data.normalise.common :refer
             [wrap-single-enums wrap-single-keywords wrap-single-type-check]]
            #_[adi.data.normalise.common-col :refer
             [wrap-attr-sets wrap-attr-vector wrap-single-vector wrap-single-list
              wrap-expression-check]]
            #_[adi.data.normalise.common-sym :refer
             [wrap-branch-underscore wrap-single-symbol]]
            #_[adi.data.normalise.allow :refer [wrap-branch-model-allow wrap-attr-model-allow]]
            #_[adi.data.normalise.convert :refer [wrap-single-model-convert]]
            #_[adi.data.normalise.expressions :refer [wrap-single-model-expressions]]
            #_[adi.data.normalise.fill-empty :refer [wrap-model-fill-empty]]
            #_[adi.data.normalise.fill-assoc :refer [wrap-model-fill-assoc]]
            #_[adi.data.normalise.ignore :refer [wrap-nil-model-ignore]]
            #_[adi.data.normalise.mask :refer [wrap-model-pre-mask wrap-model-post-mask]]
            #_[adi.data.normalise.require :refer [wrap-model-pre-require wrap-model-post-require]]
            #_[adi.data.normalise.transform :refer [wrap-model-pre-transform wrap-model-post-transform]]
            #_[adi.data.normalise.validate :refer [wrap-single-model-validate]]))

(defn db-id-syms [db env]
  (if-let [id (and db (:id db))]
    (cond (symbol? id)
          (let [nid (cond (= id '_) '_
                          (.startsWith (name id) "?") id
                          :else (symbol (str "?" (name id))))]
            (assoc db :id nid))
          :else db)))

(defn wrap-db [f]
  (fn [tdata tsch nsv interim fns env]
    (let [db (:db tdata)
          db (db-id-syms db env)
          output (f (dissoc tdata :db) tsch nsv interim fns env)]
      (if db
        (assoc output :db db)
        output))))

(defn wrap-plus [f]
  (fn [tdata tsch nsv interim fns env]
    (let [output (f (dissoc tdata :+) tsch nsv interim fns env)
          pinterim  (normalise/submaps interim normalise/tree-directives :+)]
      (if-let [tplus (:+ tdata)]
        (assoc output :+
               ((:normalise fns)
                tplus (-> env :schema :tree) [] pinterim fns env))
        output))))

(defn wrap-ref-path [f]
  (fn [tdata tsch nsv interim fns env]
    (let [rp (or (:ref-path interim) [])]
      (f tdata tsch nsv (assoc interim :ref-path (conj rp tdata)) fns env))))

(defn wrap-key-path [f]
  (fn [tdata tsch nsv interim fns env]
    (let [kp (or (:key-path interim) [])]
      (f tdata tsch nsv (assoc interim :key-path (conj kp (last nsv))) fns env))))

(defn normalise
  ([data env]
     (let [tdata (data/treeify-keys-nested data)
           tdata (if-let [pre-process-fn (-> env :model :pre-process)]
                   (pre-process-fn tdata env)
                   tdata)
           tsch (-> env :schema :tree)
           interim (:model env)
           fns {:normalise
                (let [f (-> normalise/normalise-loop
                            wrap-db)
                      f (wrap-plus f)
                      ;;f (if (-> env :model :fill-assoc)     (wrap-model-fill-assoc f) f)
                      ;;f (if (-> env :model :fill-empty)     (wrap-model-fill-empty f) f)
                      ;;f (if (-> env :model :pre-transform)  (wrap-model-pre-transform f) f)
                      ;;f (if (-> env :model :pre-mask)       (wrap-model-pre-mask f) f)
                      ;;f (if (-> env :model :pre-require)    (wrap-model-pre-require f) f)
                      ;;f (if (-> env :model :post-require)   (wrap-model-post-require f) f)
                      ;;f (if (-> env :model :post-mask)      (wrap-model-post-mask f) f)
                      ;;f (if (-> env :model :post-transform) (wrap-model-post-transform f) f)
                      f (wrap-ref-path f)
                      ;;f (wrap-alias f)
                      ]
                  f)

                :normalise-nil
                (let [f normalise/normalise-nil
                      ;;f (if (-> env :model :ignore)    (wrap-nil-model-ignore f) f)
                      ]
                  f)

                :normalise-branch
                (let [f normalise/normalise-loop
                      ;;f (if (-> env :model :allow)    (wrap-branch-model-allow f) f)
                      ;;f (wrap-branch-underscore f)
                      ;;f (wrap-key-path f)
                      ;;f (wrap-alias f)
                      ]
                  f)

                :normalise-attr
                (let [f (-> normalise/normalise-attr
                            ;;wrap-attr-sets
                            ;;wrap-attr-vector
                            )
                      ;;f (if (-> env :model :allow)     (wrap-attr-model-allow f) f)
                      f (wrap-key-path f)
                      ]
                  f)

                :normalise-expression
                (let [f normalise/normalise-expression
                      ;;f (if (-> env :model :expressions)  (wrap-single-model-expressions f) f)
                      ]
                  f)

                :normalise-single
                (let [f (-> normalise/normalise-single
                            ;;wrap-single-enums
                            ;;wrap-single-keywords
                            )
                      ;;f (if (-> env :options :model-typecheck) (wrap-single-type-check f) f)
                      ;;f (if (-> env :model :convert)  (wrap-single-model-convert f) f)
                      ;;f (if (-> env :model :validate)  (wrap-single-model-validate f) f)
                      f (-> f
                            ;;wrap-single-list
                            ;;wrap-single-symbol
                            ;;wrap-single-vector
                            )]
                  f)}
           output ((:normalise fns) tdata tsch [] interim fns env)]
       (if-let [post-process-fn (-> env :model :post-process)]
         (post-process-fn output env)
         output))))
