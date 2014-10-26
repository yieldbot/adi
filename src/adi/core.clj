(ns adi.core
  (:require [hara.namespace.import :as ns]
            [hara.common [checks :refer [boolean?]]]
            [adi.core [connection :as connection]]
            [adi.core [select :as select]]
            [adi.core [transaction :as transaction]]))

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

(ns/import adi.core.connection [connect! disconnect!])

(define-database-functions
  [select/select
   transaction/insert!
   transaction/transact!
   transaction/delete!
   transaction/update!])






(comment
  (def adi (connect! "datomic:mem://adi-test-select" {:account/name [{}]} true true))

  (insert! adi [{:account/name "Chris"}
                {:account/name "Bob"}] :raw)

  (transaction/insert! adi [{:account/name "Chris"}
                            {:account/name "Bob"}]
                       (args->opts [:raw]))
  (connect! (disconnect! adi))


  (datomic/db (:connection adi))
  (datomic/tx-range t->tx 1000))
