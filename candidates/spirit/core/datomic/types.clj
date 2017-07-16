(ns spirit.core.datomic.types
  (:require [datomic.api :as datomic]
            [hara.component :as component]
            [spirit.core.datomic.schema.generate :as generate]
            [spirit.core.datomic.schema.base :as base]
            [spirit.data.schema :as schema]
            [spirit.common.graph :as graph]))

;;(ns-unalias 'spirit.core.datomic.types 'schema)

(defn last-transaction-time
  "returns the time of last transaction for the database
   
   (last-transaction-time (datomic/db (connect \"datomic:mem://hello\")))
   => (contains [number? #(instance? java.util.Date %)])"
  {:added "0.5"}
  [ddb]
  (let [t (-> ddb datomic/basis-t)]
    [t (ffirst (datomic/q '[:find ?t
                            :in $ ?tx
                            :where [?tx :db/txInstant ?t]]
                          ddb
                          (datomic/t->tx t)))]))

(defmethod print-method datomic.peer.LocalConnection
  [conn w]
  (.write w  (if-let [db (try (datomic/db conn)
                              (catch Exception t))]
               (let [[t dt] (last-transaction-time db)]
                 (format "#conn%s" {t dt}))
               "#conn{}")))

(defmethod print-method datomic.db.Db
  [db w]
  (.write w (if-let [[t dt] (last-transaction-time db)]
              (format "#db%s" {t dt})
              "#db{}")))

(defmulti construct-uri
  "constructs the datomic uri
 
   (construct-uri {:protocol :mem
                   :name \"hello\"})
   => \"datomic:mem://hello\"
 
   (construct-uri {:protocol :dev
                   :host \"localhost\"
                   :port 4334
                   :name \"hello\"})
   => \"datomic:dev://localhost:4334/hello\"
 
     (construct-uri {:uri \"datomic:mem://hello\"})
   => \"datomic:mem://hello\""
  {:added "0.5"}
  :protocol)
  
(defmethod construct-uri :mem
  [{:keys [name]}]
  (str "datomic:mem://" name))
  
(defmethod construct-uri :dev
  [{:keys [host port name]}]
  (str "datomic:dev://" host ":" port "/" name))

(defmethod construct-uri nil
  [{:keys [uri]}]
  uri)

(defn connect
  "connect to uri, creating the database if it does not exist
 
   (connect \"datomic:mem://hello\")
   => #(instance? datomic.peer.LocalConnection %)"
  {:added "0.5"}
  [uri]
  (try (datomic/connect uri)
       (catch clojure.lang.ExceptionInfo e
         (if (= (ex-data e) {:db/error :db.error/db-not-found})
           (do (datomic/create-database uri)
               (datomic/connect uri))
           (throw e)))))

(defn delete-database
  "deletes the specified database
   
   (delete-database {:uri \"datomic:mem://hello\"})"
  {:added "0.5"}
  [db]
  (datomic/delete-database (construct-uri db))
  db)

(defn install-schema
  "installs the schema to the database
   
   (-> (map->Datomic {:schema {:account {:user [{}]}}
                      :connection  (connect \"datomic:mem://hello\")})
       (install-schema))
   ;;=> #datomic{:schema #schema{:account {:user :string}}
   ;;            :connection #conn{...}
   "
  {:added "0.5"}
  [{:keys [schema connection] :as db}]
  (let [datoms  (generate/datomic-schema (:flat schema))]
    (datomic/transact connection datoms))
  db)

(defn start-datomic
  "helper for IComponent/-start
   
   (start-datomic {:schema {:account {:user [{}]}}
                   :uri \"datomic:mem://hello\"
                   :options #{:install-schema}})
   ;;=> {:schema #schema{:account {:user :string}},
   ;;    :uri \"datomic:mem://hello\",
   ;;    :options #{:install-schema},
   ;;    :connection #conn{...}}
   "
  {:added "0.5"}
  [{:keys [options schema] :as db}]
  (let [uri  (construct-uri db)
        _    (if (:reset-db options) (datomic/delete-database uri))
        conn (connect uri)
        schema  (if (instance? spirit.data.schema.Schema schema)
                  schema
                  (schema/schema schema base/all-auto-defaults))
        db   (assoc db :uri uri :schema schema :connection conn)]
    (cond-> db
      (:install-schema options) install-schema)))

(defn stop-datomic
  "helper for IComponent/-start
 
   (-> {:uri \"datomic:mem://hello\"}
       (start-datomic)
       (stop-datomic)
       (delete-database))
   ;;=> {:uri \"datomic:mem://hello\", :schema #schema{}}
  "
  {:added "0.5"}
  [{:keys [connection] :as db}]
  (if connection
      (datomic/release connection))
  (dissoc db :connection))

(defrecord Datomic  []

  Object
  (toString [db]
    (str "#datomic" (into {} (assoc (dissoc db :functions)
                                    :uri
                                    (construct-uri db)))))
  
  component/IComponent
    
  (-start [db] (start-datomic db))
  
  (-stop  [db] (stop-datomic db)))

(defmethod print-method Datomic
  [v w]
  (.write w (str v)))

(defmethod graph/create :datomic
  [{:keys [datomic] :as m}]
  (map->Datomic m))
