(ns spirit.pipeline
  (:require [hara.common.checks :refer [hash-map? long?]]
            [hara.data.map :refer [assoc-if]]
            [hara.data.path :as data]
            [hara.event :refer [raise]]
            [hara.function.args :refer [op]]
            [hara.string.path :as path]
            [spirit.pipeline.base
             [alias :as alias]
             [enum :as enum]
             [keyword :as keyword]
             [type-check :as type-check]]
            [spirit.pipeline
             [allow :as allow]
             [convert :as convert]
             [fill-assoc :as fill-assoc]
             [fill-empty :as fill-empty]
             [ignore :as ignore]
             [mask :as mask]
             [require :as require]
             [transform :as transform]
             [validate :as validate]]))

(def tree-directives
  #{:pre-require       ;;
    :pre-mask          ;;
    :pre-transform     ;;
    :fill-assoc        ;;
    :fill-empty        ;;
    :ignore            ;;
    :allow             ;;
    :expression        ;;
    :validate          ;;
    :convert           ;;
    :post-transform    ;;
    :post-mask         ;;
    :post-require      ;;
    })

(defn submaps
  "creates a submap based upon a lookup subkey
  (submaps {:allow  {:account :check}
            :ignore {:account :check}} #{:allow :ignore} :account)
  => {:allow :check, :ignore :check}"
  {:added "0.3"}
  [m options subk]
  (reduce (fn [out option]
            (let [sv (get-in m [option subk])]
              (assoc out option sv)))
          (apply dissoc m (seq options))
          options))

(defn wrap-plus
  "Allows additional attributes (besides the link :ns) to be added to the entity
  (pipeline/normalise {:account {:orders {:+ {:account {:user \"Chris\"}}}}}
                       {:schema (schema/schema examples/account-orders-items-image)}
                       {:normalise [wrap-plus]})
  => {:account {:orders {:+ {:account {:user \"Chris\"}}}}}
  "
  {:added "0.3"}
  [f]
  (fn [tdata tsch nsv interim fns datasource]
    (let [output (f (dissoc tdata :+) tsch nsv interim fns datasource)
          pinterim  (submaps interim tree-directives :+)]
      (if-let [tplus (:+ tdata)]
        (let [pinterim (update-in pinterim [:key-path] conj :+)]
          (assoc output :+
                 ((:normalise fns) tplus (-> datasource :schema :tree) [] pinterim fns datasource)))
        output))))

(defn wrap-ref-path
  "Used for tracing the entities through `normalise`
  (pipeline/normalise {:account {:orders {:+ {:account {:WRONG \"Chris\"}}}}}
                       {:schema (schema/schema examples/account-orders-items-image)}
                       {:normalise [wrap-ref-path wrap-plus]})

  => (throws-info {:ref-path
                    [{:account {:orders {:+ {:account {:WRONG \"Chris\"}}}}}
                     {:account {:WRONG \"Chris\"}}]})"
  {:added "0.3"}
  [f]
  (fn [tdata tsch nsv interim fns datasource]
    (f tdata tsch nsv (update-in interim [:ref-path] (fnil #(conj % tdata) [])) fns datasource)))

(defn wrap-key-path
  "Used for tracing the keys through `normalise`
  (pipeline/normalise {:account {:orders {:+ {:account {:WRONG \"Chris\"}}}}}
                       {:schema (schema/schema examples/account-orders-items-image)}
                       {:normalise [wrap-plus]
                        :normalise-branch [wrap-key-path]
                        :normalise-attr [wrap-key-path]})
  =>  (throws-info {:key-path [:account :orders :+ :account]})"
  {:added "0.3"}
  [f]
  (fn [tdata tsch nsv interim fns datasource]
    (f tdata tsch nsv (update-in interim [:key-path] (fnil #(conj % (last nsv)) [])) fns datasource)))

(defn normalise-loop [tdata tsch nsv interim fns datasource]
  (reduce-kv (fn [output k subdata]
               (let [subsch (get tsch k)
                     pinterim (submaps interim tree-directives k)]
                (cond (nil? subsch)
                      (assoc-if output k
                                ((:normalise-nil fns)
                                 subdata nil (conj nsv k) pinterim datasource))

                      (hash-map? subsch)
                      (assoc-if output k
                                ((:normalise-branch fns)
                                 subdata subsch (conj nsv k) pinterim fns datasource))

                      (vector? subsch)
                      (assoc-if output k
                                ((:normalise-attr fns)
                                 subdata subsch (conj nsv k) pinterim fns datasource))
                      :else
                      (let [nnsv (conj nsv k)]
                        (raise [:normalise :wrong-input {:data subdata :nsv nnsv :key-path (:key-path interim)}]
                          (str "NORMALISE_LOOP: In " nsv ", " subdata " needs to be a vector or hashmap.")))
                      )))
             {} tdata))

(defn normalise-nil [subdata _ nsv interim datasource]
  (raise [:normalise :no-schema {:nsv nsv :key-path (:key-path interim) :ref-path (:ref-path interim)}]
         (str "NORMALISE_NIL: " nsv " is not in the schema.")))

(defn normalise-attr [subdata [attr] nsv interim fns datasource]
  (cond (set? subdata)
        (-> (map #((:normalise-single fns) % [attr] nsv interim fns datasource) subdata)
            (set)
            (disj nil))

        :else
        ((:normalise-single fns) subdata [attr] nsv interim fns datasource)))

(defn normalise-single [subdata [attr] nsv interim fns datasource]
  (if (= (:type attr) :ref)
    (cond (hash-map? subdata)
          (let [nnsv (path/split (-> attr :ref :ns))]
            ((:normalise fns)
             (data/treeify-keys-nested subdata)
             (get-in datasource (concat [:schema :tree] nnsv))
             nnsv interim fns datasource))
          
          :else
          (raise [:normalise :wrong-input {:nsv nsv :key-path (:key-path interim)}]
            (str "NORMALISE_SINGLE: In " nsv "," subdata " should be either a hashmaps or ids, not ")))
    subdata))

(defn normalise-expression [subdata [attr] nsv interim datasource] subdata)

(defn normalise-wrap [fns wrappers]
  (reduce-kv (fn [out k f]
               (let [nf (if-let [wrapvec (get wrappers k)]
                          (reduce (fn [f wrapper] (wrapper f)) f wrapvec)
                          f)]
                 (assoc out k nf)))
             {} fns))

(def normalise-wrapper-fns
  {:plus            wrap-plus
   :fill-assoc      fill-assoc/wrap-model-fill-assoc
   :fill-empty      fill-empty/wrap-model-fill-empty
   :pre-transform   transform/wrap-model-pre-transform
   :pre-mask        mask/wrap-model-pre-mask
   :pre-require     require/wrap-model-pre-require
   :post-require    require/wrap-model-post-require
   :post-mask       mask/wrap-model-post-mask
   :post-transform  transform/wrap-model-post-transform
   :ref-path        wrap-ref-path
   :key-path        wrap-key-path
   :alias           alias/wrap-alias
   :ignore          ignore/wrap-nil-model-ignore
   :allow-branch    allow/wrap-branch-model-allow
   :allow-attr      allow/wrap-attr-model-allow
   :keyword         keyword/wrap-single-keyword
   :convert         convert/wrap-single-model-convert
   :validate        validate/wrap-single-model-validate
   :enum            enum/wrap-single-enum
   :type-check      type-check/wrap-single-type-check})

(defn normalise-wrappers
  ([datasource]
   (normalise-wrappers datasource {} normalise-wrapper-fns))
  ([{:keys [pipeline options]} additions fns]
   (->> {:normalise  [:plus
                      (if (:fill-assoc pipeline)     :fill-assoc)
                      (if (:fill-empty pipeline)     :fill-empty)
                      (if (:pre-transform pipeline)  :pre-transform)
                      (if (:pre-mask pipeline)       :pre-mask)
                      (if (:pre-require pipeline)    :pre-require)
                      (if (:post-require pipeline)   :post-require)
                      (if (:post-mask pipeline)      :post-mask)
                      (if (:post-transform pipeline) :post-transform)
                      :ref-path
                      :alias]
         :normalise-nil     [   (if (:ignore pipeline) :ignore)]
         :normalise-branch     [(if (:allow pipeline)  :allow-branch)
                                :alias
                                :key-path]
         :normalise-attr       [(if (:allow pipeline) :allow-attr)
                                :key-path]
         
         :normalise-expression []
         
         :normalise-single     [:enum
                                :keyword
                                (if (:use-typecheck options) :type-check)
                                (if (:convert pipeline) :convert)
                                (if (:validate pipeline) :validate)]}
        (reduce-kv (fn [out k v]
                     (assoc out k (->> (concat (get-in additions [k :pre])
                                               v
                                               (get-in additions [k :post]))
                                       (keep identity)
                                       (map fns))))
                   {}))))

(defn normalise
  ([data {:keys [pipeline] :as datasource}]
   (let [wrappers (normalise-wrappers datasource)]
     (normalise data datasource wrappers)))
  ([data datasource wrappers]
   (let [tdata (data/treeify-keys-nested data)
         tdata (if-let [pre-process-fn (-> datasource :pipeline :pre-process)]
                 (op pre-process-fn tdata datasource)
                 tdata)
         tsch (-> datasource :schema :tree)
         interim (:pipeline datasource)
         fns {:normalise normalise-loop
              :normalise-nil normalise-nil
              :normalise-branch normalise-loop
              :normalise-attr normalise-attr
              :normalise-expression normalise-expression
              :normalise-single normalise-single}
         fns (normalise-wrap fns wrappers)
         output ((:normalise fns) tdata tsch [] interim fns datasource)]
     (if-let [post-process-fn (-> datasource :pipeline :post-process)]
       (post-process-fn output datasource)
       output))))
