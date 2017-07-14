(ns spirit.rabbitmq
  (:require [spirit.rabbitmq
             [api :as api]
             [request :as request]]
            [spirit.common.queue :as common]
            [spirit.protocol.iqueue :as queue]
            [hara.component :as component])
  (:import java.net.URLEncoder))

(def ^:dynamic *default-options*
  {:protocol "http"
   :host "localhost"
   :port 5672
   :management-port 15672
   :username "guest"
   :password "guest"
   :vhost "/"})

(def ^:dynamic *default-exchanges*
  #{""
    "amq.direct" "amq.fanout" "amq.headers"
    "amq.match" "amq.rabbitmq.trace"
    "amq.rabbitmq.log" "amq.topic"})

(defn routing-all
  "lists all the routing in the mq
 
   (routing-all (common/create {:type :rabbitmq
                               :refresh true})
                {})
   => {\"/\" {:queues {}, :exchanges {}, :bindings {}}}"
  {:added "0.5"}
  [rabbitmq opts]
  (let [vhosts  (->> (api/list-vhosts rabbitmq)
                     (mapv :name))]
    (->> (map #(common/routing (assoc rabbitmq :vhost-encode (URLEncoder/encode %))) vhosts)
         (zipmap vhosts))))

(defn network
  "returns the mq network
 
   (network (common/create {:type :rabbitmq
                           :refresh true}))
   => (contains-in {:cluster-name string?
                    :nodes [string?]
                    :vhosts [\"/\"]
                    :connections ()
                   :channels {}})"
  {:added "0.5"}
  [rabbitmq]
  (let [vhosts  (->> (api/list-vhosts rabbitmq)
                     (mapv :name))
        connections (->> (api/list-connections rabbitmq)
                         (map #(select-keys % [:name :peer-port :peer-host :host :port])))
        channels    (->> (api/list-channels rabbitmq)
                         (reduce (fn [out data]
                                   (update-in out [(-> data :connection-details :name)] (fnil conj #{})))
                                 {}))
        cluster-name (:name (api/cluster-name rabbitmq))
        nodes        (->> (api/list-nodes rabbitmq)
                          (mapv :name))]
    {:cluster-name cluster-name
     :nodes nodes
     :vhosts vhosts
     :connections connections
     :channels channels}))

(defrecord RabbitMQ []
  Object
  (toString [mq]
    (str "#rabbit" (common/routing mq {:short true})))
  
  component/IComponent
  (-start [{:keys [routing consumers refresh] :as mq}]
    (cond-> mq
       refresh   (common/purge-routing)
       routing   (common/install-routing routing)
       consumers (common/install-consumers consumers)))
  
  (-stop [mq]
    mq)

  queue/IQueue
  (-list-queues     [mq]
    (->> (api/list-queues mq)
         (reduce (fn [out {:keys [name] :as data}]
                   (assoc out name (select-keys data [:exclusive :auto-delete :durable])))
                 {})))
  (-add-queue       [mq name opts]
    (api/add-queue mq name opts)
    mq)
  
  (-delete-queue    [mq name]
    (api/delete-queue mq name)
    mq)
  
  (-list-exchanges  [mq]
    (->> (api/list-exchanges mq)
         (remove #(-> % :name *default-exchanges*))
         (reduce (fn [out {:keys [name] :as data}]
                   (assoc out name (select-keys data [:type :internal :auto-delete :durable])))
                 {})))
  (-add-exchange    [mq name opts]
    (api/add-exchange mq name opts)
    mq)
  
  (-delete-exchange [mq name]
    (api/delete-exchange mq name)
    mq)

  (-list-bindings   [mq]
    (->> (api/list-bindings mq)
         (remove #(-> % :source empty?))
         (reduce (fn [out {:keys [source destination destination-type] :as data}]
                   (update-in out [source (keyword (str destination-type "s")) destination]
                              (fnil #(conj % (dissoc data :source :vhost :destination :destination-type))
                                    [])))
                 {})))

  (-bind-exchange   [mq source dest opts]
    (api/bind-exchange mq source dest opts)
    mq)
  
  (-bind-queue      [mq source dest opts]
    (api/bind-queue mq source dest opts)
    mq)

  (-list-consumers  [mq]
    (->> (api/list-consumers mq)
         (map (fn [m]
                {:queue (-> m :queue :name)
                 :id (:consumer-tag m)
                 :details (:channel-details m)}))
         (reduce (fn [out {:keys [queue id details]}]
                   (assoc-in out [queue (keyword id)] details))
                 {})))
  
  (-add-consumer    [mq name handler]
    (throw (Exception. "NOT IMPLEMENTED")))
  
  (-delete-consumer [mq name id]
    (throw (Exception. "NOT IMPLEMENTED")))

  (-publish         [mq exchange message opts]
    (api/add-message mq exchange {:routing-key (or (:key opts) "")
                                  :payload message
                                  :payload-encoding (or (:encoding opts) "string")
                                  :properties (dissoc opts :key :encoding)})))

(defmethod print-method RabbitMQ
  [v w]
  (.write w (str v)))

(defn install-vhost
  "installs vhost and adds user permissions"
  {:added "0.5"}
  [{:keys [vhost username] :as rabbitmq}]
  (let [curr (->> (api/list-vhosts rabbitmq)
                  (map :name)
                  set)]
    (when (not (curr vhost))
      (api/add-vhost vhost {})
      (api/add-permissions rabbitmq username {:configure ".*"
                                              :write ".*"
                                              :read ".*"}))))

(defmethod common/create :rabbitmq
  [m]
  (let [m (merge m *default-options*)]
    (-> (map->RabbitMQ m)
        (assoc :vhost-encode (URLEncoder/encode (:vhost m))))))

(defn rabbit
  "creates a rabbitmq instance"
  {:added "0.5"}
  ([] (rabbit {}))
  ([m]
   (-> (common/create {:type :rabbitmq})
       (component/start))))
