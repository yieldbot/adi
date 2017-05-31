(ns spirit.datomic
  (:require [hara.namespace.import :as ns]
            [hara.common [checks :refer [boolean?]]]
            [spirit.datomic.core
             [helpers :as helpers]
             [prepare :as prepare]
             [nested :as nested]
             [retract :as retract]
             [select :as select]
             [transaction :as transaction]]
            [spirit.common.schema :as schema]
            [spirit.datomic.schema.base :as base]
            [spirit.datomic.data :as data]
            [spirit.datomic.types :as types]
            [hara.component :as component]))

(ns/import spirit.datomic.core.helpers    [transactions transaction-time schema-properties]
           spirit.common.schema           [schema]
           spirit.datomic.data            [iid])

(def reserved
  #{:options
    :pull
    :access
    :return
    :transact
    :at
    :db
    :op
    :pipeline
    :simulate
    :schema
    :connection
    :profiles})

(def options
  #{:ban-expressions
    :ban-ids
    :ban-top-id
    :ban-body-ids
    :schema-required
    :schema-restrict
    :schema-defaults
    :pipeline-typecheck
    :pipeline-coerce
    :skip-normalise
    :skip-typecheck
    :first
    :ids
    :generate-ids
    :generate-syms
    :blank
    :raw
    :debug})

(defn connect!
  ([{:keys [uri schema reset? install-schema?] :as args}]
   (let [opts (dissoc args :uri :schema :reset? :install-schema?)]
     (connect! uri schema reset? install-schema? opts)))
  ([uri schema & [reset? install-schema? opts]]
   (let [m (cond (string? uri) {:uri uri}
                 (map? uri) uri)
         schema (schema/schema schema base/all-auto-defaults)]
     (-> (merge m opts)
         (assoc :schema schema)
         (types/datomic)
         (component/start {:hooks {:pre-start (if reset? [:delete-database])
                                   :post-start (if install-schema? [:install-schema])}})))))

(defn disconnect! [datasource]
  (component/stop datasource))

(defn args->opts
  ([args] (args->opts {} args))
  ([output [x y & xs :as args]]
     (cond (nil? x) output
           (or (and (options x) (not (boolean? y)))
               (and (nil? y) (nil? xs)))
           (recur (update-in output [:options] assoc x true) (next args))

           (and (options x) (boolean? y))
           (recur (update-in output [:options] assoc x true) xs)

           :else (recur (assoc output x y) xs))))

(defn create-function-template [f]
  (let [nsp   (-> f meta :ns (.toString))
        name  (-> f meta :name)
        fargs (-> f meta :arglists first butlast vec)]
    `(defn ~name ~(-> fargs (conj '& 'args))
       (let [~'farg (first ~'args)
             ~'opts (if (map? ~'farg)
                      ~'farg
                      (args->opts ~'args))]
         (~(symbol (str nsp "/" name)) ~@fargs ~'opts)))))

(defmacro define-database-functions [functions]
  (->> functions
       (map resolve)
       (map create-function-template)
       (vec)))

(define-database-functions
  [select/select
   select/query
   transaction/insert!
   transaction/transact!
   transaction/delete!
   transaction/delete-all!
   transaction/update!

   retract/retract!
   nested/update-in!
   nested/delete-in!
   nested/retract-in!])

(def transaction-ops
  #{#'transact!
    #'insert!
    #'delete!
    #'delete-all!
    #'update!

    #'retract!
    #'retract-in!
    #'update-in!
    #'delete-in!})

(defn create-data-form [form datasource]
  (let [[f & args] form]
    (if (transaction-ops (resolve f))
      (concat (list f datasource) args (list :raw))
      (throw (AssertionError. (str "Only " transaction-ops " allowed."))))))

(defmacro sync-> [datasource args? & trns]
  (let [[opts trns] (if (vector? args?)
                      [(args->opts args?) trns] [{} (cons args? trns)])
        sourcesym (gensym)
        forms (filter identity (map #(create-data-form % sourcesym) trns))]
    `(let [~sourcesym  (prepare/prepare ~datasource (assoc ~opts :transact :datomic) nil)]
       (transact! ~sourcesym (concat ~@forms)))))
