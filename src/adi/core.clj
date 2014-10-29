(ns adi.core
  (:require [hara.namespace.import :as ns]
            [hara.common [checks :refer [boolean?]]]
            [adi.core
             [connection :as connection]
             [helpers :as helpers]
             [prepare :as prepare]
             [nested :as nested]
             [query :as query]
             [retract :as retract]
             [select :as select]
             [transaction :as transaction]]))

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
    :return-ids
    :return-entities
    :generate-ids
    :generate-syms
    :raw
    :simulate})

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
           adi.core.helpers    [transactions transaction-time])

(define-database-functions
  [select/select
   transaction/insert!
   transaction/transact!
   transaction/update!
   transaction/delete!
   transaction/delete-all!
   retract/retract!
   query/query
   nested/update-in!
   nested/delete-in!
   nested/retract-in!])

(def transaction-ops
  #{#'transact! #'insert! #'update! #'delete! #'retract!
    #'retract-in! #'update-in! #'delete-in! #'delete-all!})

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
    `(let [~adisym  (prepare/prepare ~adi ~opts)]
       (transact! ~adisym (concat ~@forms)))))
