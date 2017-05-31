(ns spirit.datomic.types
  (:require [datomic.api :as datomic]
            [hara.component :as component]
            [spirit.datomic.schema.generate :as generate]
            [spirit.datomic.schema.base :as base]
            [spirit.common.schema :as schema]))

;;(ns-unalias 'spirit.datomic.types 'schema)

(defn last-transaction-time [db]
  (let [t (-> db datomic/basis-t)]
    [t (ffirst (datomic/q '[:find ?t
                            :in $ ?tx
                            :where [?tx :db/txInstant ?t]]
                          db
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


(defmulti construct-uri :protocol)
  
(defmethod construct-uri :mem
  [{:keys [name]}]
  (str "datomic:mem://" name))
  
(defmethod construct-uri :dev
  [{:keys [host port name]}]
  (str "datomic:dev://" host ":" port "/" name))

(defmethod construct-uri nil
  [{:keys [uri]}]
  uri)

(defn connect [uri]
  (try (datomic/connect uri)
       (catch clojure.lang.ExceptionInfo e
         (if (= (ex-data e) {:db/error :db.error/db-not-found})
           (do (datomic/create-database uri)
               (datomic/connect uri))
           (throw e)))))

(defrecord Datomic  []

  Object
  (toString [this]
    (str "#datomic" (into {} (dissoc this :functions))))
  
  component/IComponent
    
  (-start [datomic]
    (let [uri  (or (:uri datomic) (construct-uri datomic))
          conn (connect uri)]
      (assoc datomic :connection conn)))
  
  (-stop  [datomic]
    (if-let [conn (:connection datomic)]
      (datomic/release conn))
    (dissoc datomic :connection)))

(defmethod print-method Datomic
  [v w]
  (.write w (str v)))

(defn create-schema [schema]
  (if (instance? spirit.common.schema.Schema schema)
    schema
    (schema/schema schema base/all-auto-defaults)))

(defn create-datoms [schema]
  (generate/datomic-schema (:flat schema)))

(defn delete-database [datomic]
  (datomic/delete-database (construct-uri datomic))
  datomic)

(defn install-schema [{:keys [schema connection] :as datomic}]
  (let [schema  (create-schema schema)
        datoms  (create-datoms schema)]
    (datomic/transact connection datoms)
    (assoc datomic :schema schema)))

(defn datomic [config]
  (assoc (map->Datomic config)
         :functions {:delete-database delete-database
                     :install-schema  install-schema}))

(comment
  (def topology {:db [{:constructor datomic
                       :hooks {:pre-start  [:delete-database]
                               :post-start [:install-schema]
                               :post-stop  [:delete-database]}}
                      :schema]
                 
                 :schema [create-schema]})
  
  (def config   {:db  {:type     :datomic
                       :protocol :mem
                       :name "spirit-test"

                       ;;:protocol :dev
                       ;;:host "localhost"
                       ;;:port 4334
                       }
                 :schema {:account {:name [{}]}}})
  
  (datomic/delete-database (construct-uri (map->Datomic (:db config))))
  (def system (component/stop (component/start (component/system topology config))))
  
  (datomic/connect (construct-uri (:db config)))
  (meta (:db (component/system topology config)))
  (:connection (:db system))
  

  )
