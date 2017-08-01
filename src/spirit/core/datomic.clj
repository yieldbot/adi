(ns spirit.core.datomic
  (:require [hara.namespace.import :as ns]
            [hara.common [checks :refer [boolean?]]]
            [hara.data.nested :refer [merge-nested merge-nil-nested]]
            [spirit.core.datomic.api
             [helpers :as helpers]
             [prepare :as prepare]
             [nested :as nested]
             [retract :as retract]
             [select :as select]
             [transaction :as transaction]]
            [spirit.data.schema :as schema]
            [spirit.protocol.igraph :as graph]
            [spirit.core.datomic.schema.base :as base]
            [spirit.core.datomic.data :as data]
            [spirit.core.datomic.types :as types]
            [hara.component :as component]))

(ns/import spirit.core.datomic.api.helpers    [transactions transaction-time schema-properties]
           spirit.data.schema           [schema]
           spirit.core.datomic.data            [iid])

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
  "creates a datomic connection to a database
 
   (def ds (connect! {:protocol :mem
                     :name \"spirit.core.datomic-test\"
                      :schema {:account {:user [{:required true}]}}
                      :options {:ids false
                                :reset-db true
                                :install-schema true}}))"
  {:added "0.5"}
  ([m]
   (-> (types/map->Datomic m)
       (assoc :type :datomic)
       (component/start)))
  ([uri schema & [reset-db install-schema opts]]
   (connect! (merge-nested {:uri uri
                            :schema schema
                            :options {:ids true
                                      :reset-db reset-db
                                      :install-schema install-schema}}
                           opts))))

(defn disconnect!
  "releases the datomic connection
 
   (def ds (connect! \"datomic:mem://spirit.core.datomic-test\"
                     {:account {:user [{:required true}]}}
                     true true))
   (disconnect! ds)"
  {:added "0.5"}
  [db]
  (component/stop db))

(defn args->opts
  "makes a map from input arguments
 
   (args->opts [:raw false :first :ids :transact :datasource])
   => {:options {:raw false,
                 :first true,
                 :ids true},
       :transact :datasource}"
  {:added "0.5"}
  ([args] (args->opts {} args))
  ([output [x y & xs :as args]]
     (cond (nil? x) output
           (or (and (options x) (not (boolean? y)))
               (and (nil? y) (nil? xs)))
           (recur (update-in output [:options] assoc x true) (next args))

           (and (options x) (boolean? y))
           (recur (update-in output [:options] assoc x y) xs)

           :else (recur (assoc output x y) xs))))

(defn create-function-template
  "helper function to define-database-functions"
  {:added "0.5"}
  [f]
  (let [nsp   (-> f meta :ns (.toString))
        name  (-> f meta :name)
        fargs (-> f meta :arglists first butlast vec)]
    `(defn ~name ~(-> fargs (conj '& 'args))
       (let [~'farg (first ~'args)
             ~'opts (if (map? ~'farg)
                      ~'farg
                      (args->opts ~'args))]
         (~(symbol (str nsp "/" name)) ~@fargs ~'opts)))))

(defmacro define-database-functions
  "scaffolding to create user friendly datomic functions"
  {:added "0.5"}
  [functions]
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

(defn create-data-form
  "helper function to sync-> for creating datom outputs"
  {:added "0.5"}
  [form datasource]
  (let [[f & args] form]
    (if (transaction-ops (resolve f))
      (concat (list f datasource) args (list :raw))
      (throw (AssertionError. (str "Only " transaction-ops " allowed."))))))

(defmacro sync->
  "makes a map from input arguments
   
   (-> (test-instance)
       (insert! [{:account/email \"foo\"}
                 {:account {:email \"bar\" :firstname \"baz\"}}]
                {:transact :datasource})
       (sync-> {:transact :datasource}
               (insert! {:account/email \"baz\"})
               (update!  {:account/email \"bar\"} {:account/lastname \"foobar\"})
               (retract! {:account/email \"bar\"} [:account/firstname])
              (delete! {:account/email \"foo\"}))
       (select {:account/email '_} {:options {:ids false}}))
   => #{{:account {:email \"bar\", :lastname \"foobar\"}}
        {:account {:email \"baz\"}}}"
  {:added "0.5"}
  [datasource args? & trns]
  (let [[opts trns] (cond (map? args?)
                          [args? trns]
                          
                          (vector? args?)
                          [(args->opts args?) trns]

                          :else
                          [{} (cons args? trns)])
        sourcesym (gensym)
        forms (filter identity (map #(create-data-form % sourcesym) trns))]
    `(let [~sourcesym  (prepare/prepare ~datasource (merge {:transact :datomic} ~opts) nil)]
       (transact! ~sourcesym (concat ~@forms)))))

(extend-type spirit.core.datomic.types.Datomic

  graph/IGraph
  (-empty [db opts]
    (->  db
        (types/delete-database)
        (component/stop)
        (component/start)
        (types/install-schema)))
  
  (-select  [db selector opts]
    (select/select db selector opts))
  
  (-insert  [db data opts]
    (transaction/insert! db data (merge-nested {:transact :datasource} opts)))
  
  (-delete  [db selector opts]
    (transaction/delete! db selector (merge-nested {:transact :datasource} opts)))

  (-retract [db selector keys opts]
    (retract/retract! db selector keys (merge-nested {:transact :datasource} opts)))

  (-update  [db selector data opts]
    (transaction/update! db selector data (merge-nested {:transact :datasource} opts))))

(defn test-instance
  "creates a test in-memory database"
  {:added "0.5"}
  ([] (test-instance {}))
  ([{:keys [name schema] :as m}]
   (connect! {:protocol :mem
              :name   (or name (str "instance-" (java.util.UUID/randomUUID)))
              :schema (or schema
                          {:account {:email [{}]
                                     :firstname [{}]
                                     :lastname [{}]}})
              :options {:install-schema true
                        :ids true}})))
