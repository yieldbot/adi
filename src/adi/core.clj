(ns adi.core
  (:require [hara.namespace.import :as ns]
            [hara.common [checks :refer [boolean?]]]
            [adi.core
             [connection :as connection]
             [helpers :as helpers]
             [prepare :as prepare]
             [nested :as nested]
             [retract :as retract]
             [select :as select]
             [transaction :as transaction]]))

(def reserved
  #{:options
    :pull
    :access
    :return
    :transact
    :at
    :db
    :op
    :model
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
    :model-typecheck
    :model-coerce
    :skip-normalise
    :skip-typecheck
    :first
    :ids
    :simulate
    :generate-ids
    :generate-syms
    :raw
    :adi})

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
       (let [~'opts (args->opts ~'args)]
         (~(symbol (str nsp "/" name)) ~@fargs ~'opts)))))

(defmacro define-database-functions [functions]
  (->> functions
       (map resolve)
       (map create-function-template)
       (vec)))

(ns/import adi.core.connection [connect! disconnect!]
           adi.core.helpers    [transactions transaction-time schema-properties])

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

(defn create-data-form [form adi]
  (let [[f & args] form]
    (if (transaction-ops (resolve f))
      (concat (list f adi) args (list :raw))
      (throw (AssertionError. (str "Only " transaction-ops " allowed."))))))

(defmacro sync-> [adi args? & trns]
  (let [[opts trns] (if (vector? args?)
                      [(args->opts args?) trns] [{} (cons args? trns)])
        adisym (gensym)
        forms (filter identity (map #(create-data-form % adisym) trns))]
    `(let [~adisym  (prepare/prepare ~adi (assoc ~opts :transact :datomic) nil)]
       (transact! ~adisym (concat ~@forms)))))
