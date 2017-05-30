(ns spirit.api
  (:require [hara.component :as component]
            [datomic.api :as datomic]))

(comment

  (def uri "datomic:sql://spirit?jdbc:postgresql://localhost:5432?user=spirit&password=spirit")
  
  (datomic/connect uri)
  (datomic/create-database uri)
  
  (def uri "datomic:ddb-local://localhost:8001/spirit/spirit?aws_access_key_id=12345&aws_secret_key=12345")
  
  (datomic/create-database uri)
  (datomic/connect uri)

  (def ds (spirit/connect! "datomic:mem://spirit-examples-step-1" schema-1 true true)))

(comment
  (defn disconnect! [spirit]
    (if-let [conn (:connection spirit)]
      (datomic/release conn))
    (dissoc spirit :connection))

  (defn connect [uri]
    (try (datomic/connect uri)
         (catch clojure.lang.ExceptionInfo e
           (if (= (ex-data e) {:db/error :peer/db-not-found})
             (do (datomic/create-database uri)
                 (datomic/connect uri))
             (throw e)))))


  (defn connect!
    ([{:keys [uri schema reset? install-schema?] :as args}]
     (let [pargs (dissoc args :uri :schema :reset? :install-schema?)]
       (connect! uri schema reset? install-schema? pargs)))
    ([uri schema & [reset? install-schema? more]]
     (when reset?
       (datomic/delete-database uri)
       (datomic/create-database uri))

     (let [conn (try (datomic/connect uri)
                     (catch clojure.lang.ExceptionInfo e
                       (if (= (ex-data e) {:db/error :peer/db-not-found})
                         (do (datomic/create-database uri)
                             (datomic/connect uri))
                         (throw e))))
           schema  (if (instance? spirit.schema.Schema schema)
                     schema
                     (schema/schema schema))
           _      (if install-schema?
                    (let [dschema (-> schema :flat datomic)]
                      (datomic/transact conn dschema)))]
       (-> (types/map->Adi {:meta {:uri uri :reset? reset? :install-schema? install-schema?}
                            :connection conn
                            :schema schema})
           (merge more)))))

  (defn display-datomic [m] {})

  (defmulti build :type)

  (defmethod build :datomic
    [{:keys [type datomic] :as opts}]
    (component/system {:config [identity]}
                      {:config datomic}
                      {:tag (name type)
                       :display display-datomic}))

  (defn datasource [{:keys [start] :as opts}]
    (let [component (build opts)]
      (if start
        (component/start component)
        component)))
  
  (defmulti construct-uri :protocol)
  
  (defmethod construct-uri :mem
    [{:keys [name]}]
    (str "datomic:mem://" name))
  
  (defmethod construct-uri :dev
    [{:keys [host port name]}]
    (str "datomic:dev://" host ":" port "/" name))

  (defn connect [uri]
    (try (datomic/connect uri)
         (catch clojure.lang.ExceptionInfo e
           (if (= (ex-data e) {:db/error :db.error/db-not-found})
             (do (datomic/create-database uri)
                 (datomic/connect uri))
             (throw e)))))
  
  (defrecord Datomic  []

    component/IComponent
    (-start [datomic]
      (let [uri  (construct-uri datomic)
            conn (connect uri)]
        (assoc datomic :connection conn :uri uri)))
      
    (-stop  [datomic]
      (if-let [conn (:connection datomic)]
        (datomic/release conn))
      (dissoc datomic :connection :uri)()))
  
  (component/start (map->Datomic {:type     :datomic
                                  :protocol :mem
                                  :name "spirit-test"}))

  
  (defn connect! [opts]
    (component/start (map->Datomic opts)))
  
  
  
  (comment
    
    
    (component/start sys)
    
    
    (def schema {})

    
    
    (def datomic (datasource {:type    :datomic
                              :protocol :dev
                              :host "localhost"
                              :port 4334
                              :name "spirit-test"}))
    


    )
  )
