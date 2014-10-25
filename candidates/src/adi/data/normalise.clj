(ns adi.data.normalise
  (:require [hara.common :refer [hash-map? hash-set? long? keyword-split assoc-if]]
            [hara.collection.hash-map :refer [treeify-keys-nested]]
            [adi.schema.types :refer [db-id?]]
            [ribol.core :refer [raise]]
            [adi.common :refer [iid]]
            [adi.data.normalise.alias :refer [wrap-alias]]
            [adi.data.normalise.common :refer
             [wrap-single-enums wrap-single-keywords wrap-single-type-check]]
            [adi.data.normalise.common-col :refer
             [wrap-attr-sets wrap-attr-vector wrap-single-vector wrap-single-list
              wrap-expression-check]]
            [adi.data.normalise.common-sym :refer
             [wrap-branch-underscore wrap-single-symbol]]
            [adi.data.normalise.allow :refer [wrap-branch-model-allow wrap-attr-model-allow]]
            [adi.data.normalise.convert :refer [wrap-single-model-convert]]
            [adi.data.normalise.expressions :refer [wrap-single-model-expressions]]
            [adi.data.normalise.fill-empty :refer [wrap-model-fill-empty]]
            [adi.data.normalise.fill-assoc :refer [wrap-model-fill-assoc]]
            [adi.data.normalise.ignore :refer [wrap-nil-model-ignore]]
            [adi.data.normalise.mask :refer [wrap-model-pre-mask wrap-model-post-mask]]
            [adi.data.normalise.require :refer [wrap-model-pre-require wrap-model-post-require]]
            [adi.data.normalise.transform :refer [wrap-model-pre-transform wrap-model-post-transform]]
            [adi.data.normalise.validate :refer [wrap-single-model-validate]]))

(declare normalise)

(def normalise-tree-directives
  #{:pre-require       ;;
    :pre-mask          ;;
    :pre-transform     ;;
    :fill-assoc        ;;
    :fill-empty        ;;
    :ignore            ;;
    :allow             ;;
    :expressions       ;;
    :validate          ;;
    :convert           ;;
    :post-transform    ;;
    :post-mask         ;;
    :post-require      ;;
    })

(defn get-submaps [m options subk]
  (let [svs (map #(get-in m [% subk]) options)]
    (loop [ks  options
           svs svs
           output (apply dissoc m (seq options))]
      (if (empty? svs) output
          (recur (next ks) (next svs)
                 (if-let [v (first svs)]
                   (assoc output (first ks) v)
                   output))))))

(defn db-id-syms [db env]
  (when db
    (if-let [id (:id db)]
      (cond (symbol? id)
            (let [nid (cond (= id '_) '_
                            (.startsWith (name id) "?") id
                             :else (symbol (str "?" (name id))))]
              (assoc db :id nid))
            :else db))))

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
          pinterim  (get-submaps interim normalise-tree-directives :+)]
      (if-let [tplus (:+ tdata)]
        (assoc output :+
               ((:normalise fns)
                tplus (-> env :schema :tree) [] pinterim fns env))
        output))))

(defn wrap-ref-path [f]
  (fn [tdata tsch nsv interim fns env]
    (if-let [rp (or (:ref-path interim) [])]
      (f tdata tsch nsv (assoc interim :ref-path (conj rp tdata)) fns env))))

(defn wrap-key-path [f]
  (fn [tdata tsch nsv interim fns env]
    (let [kp (or (:key-path interim) [])]
      (f tdata tsch nsv (assoc interim :key-path (conj kp (last nsv))) fns env))))

(defn normalise-loop
  ([tdata tsch nsv interim fns env]
     (normalise-loop tdata tsch nsv interim fns env {}))
  ([tdata tsch nsv interim fns env output]
     (if-let [[k subdata] (first tdata)]
       (recur (next tdata) tsch nsv interim fns env
              (let [subsch (get tsch k)
                    pinterim (get-submaps interim normalise-tree-directives k)]
                (cond (nil? subsch)
                      (assoc-if output k
                                ((:normalise-nil fns)
                                 subdata (conj nsv k) pinterim env))

                      (hash-map? subsch)
                      (assoc-if output k
                                ((:normalise-branch fns)
                                 subdata subsch (conj nsv k) pinterim fns env))

                      (vector? subsch)
                      (assoc-if output k
                                ((:normalise-attr fns)
                                 subdata subsch (conj nsv k) pinterim fns env))
                      :else
                      (let [nnsv (conj nsv k)]
                        (raise [:adi :normalise :wrong-input {:data subdata :nsv nnsv :key-path (:key-path interim)}]
                          (str "NORMALISE_LOOP: In " nsv ", " subdata " needs to be a vector or hashmap.")))
                      )))
       output)))

(defn normalise-nil [subdata nsv interim env]
  (raise [:adi :normalise :no-schema {:nsv nsv :key-path (:key-path interim)}]
         (str "NORMALISE_NIL: " nsv " is not in the schema.")))


(defn normalise-attr [subdata [attr] nsv interim fns env]
  (cond (hash-set? subdata)
        (disj (set (map #((:normalise-single fns) % [attr] nsv interim fns env) subdata))
              nil)

        :else
        ((:normalise-single fns) subdata [attr] nsv interim fns env)))

(defn normalise-single [subdata [attr] nsv interim fns env]
  (if (= (:type attr) :ref)
    (cond (hash-map? subdata)
          (let [nnsv (keyword-split (-> attr :ref :ns))]
            ((:normalise fns)
             (treeify-keys-nested subdata)
             (get-in env (concat [:schema :tree] nnsv))
             nnsv interim fns env))

          (or (long? subdata) (db-id? subdata))
          subdata

          :else
          (raise [:adi :normalise :wrong-input {:nsv nsv :key-path (:key-path interim)}]
            (str "NORMALISE_SINGLE: In " nsv "," subdata " should be either a hashmaps or ids, not ")))
    subdata))

(defn normalise-expression [subdata [attr] nsv interim env] subdata)

(defn normalise
  ([data env]
     (let [tdata (treeify-keys-nested data)
           tdata (if-let [pre-process-fn (-> env :model :pre-process)]
                   (pre-process-fn tdata env)
                   tdata)
           tsch (-> env :schema :tree)
           interim (:model env)
           fns {:normalise
                (let [f (-> normalise-loop
                            wrap-db)
                      f (wrap-plus f)
                      f (if (-> env :model :fill-assoc)     (wrap-model-fill-assoc f) f)
                      f (if (-> env :model :fill-empty)     (wrap-model-fill-empty f) f)
                      f (if (-> env :model :pre-transform)  (wrap-model-pre-transform f) f)
                      f (if (-> env :model :pre-mask)       (wrap-model-pre-mask f) f)
                      f (if (-> env :model :pre-require)    (wrap-model-pre-require f) f)
                      f (if (-> env :model :post-require)   (wrap-model-post-require f) f)
                      f (if (-> env :model :post-mask)      (wrap-model-post-mask f) f)
                      f (if (-> env :model :post-transform) (wrap-model-post-transform f) f)
                      f (wrap-ref-path f)
                      f (wrap-alias f)
                      ]
                  f)

                :normalise-nil
                (let [f normalise-nil
                      f (if (-> env :model :ignore)    (wrap-nil-model-ignore f) f)
                      ]
                  f)

                :normalise-branch
                (let [f normalise-loop
                      f (if (-> env :model :allow)    (wrap-branch-model-allow f) f)
                      f (wrap-branch-underscore f)
                      f (wrap-key-path f)
                      f (wrap-alias f)]
                  f)

                :normalise-attr
                (let [f (-> normalise-attr
                            wrap-attr-sets
                            wrap-attr-vector)
                      f (if (-> env :model :allow)     (wrap-attr-model-allow f) f)
                      f (wrap-key-path f)
                      ]
                  f)

                :normalise-expression
                (let [f normalise-expression
                      f (if (-> env :model :expressions)  (wrap-single-model-expressions f) f)]
                  f)

                :normalise-single
                (let [f (-> normalise-single
                            wrap-single-enums
                            wrap-single-keywords)
                      f (if (-> env :options :model-typecheck) (wrap-single-type-check f) f)
                      f (if (-> env :model :convert)  (wrap-single-model-convert f) f)
                      f (if (-> env :model :validate)  (wrap-single-model-validate f) f)
                      f (-> f
                            wrap-single-list
                            wrap-single-symbol
                            wrap-single-vector)]
                  f)}
           output ((:normalise fns) tdata tsch [] interim fns env)]
       (if-let [post-process-fn (-> env :model :post-process)]
         (post-process-fn output env)
         output))))
