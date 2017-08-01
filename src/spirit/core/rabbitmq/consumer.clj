(ns spirit.core.rabbitmq.consumer
  (:require [spirit.core.rabbitmq
             [api :as api]
             [request :as request]])
  (:import [com.rabbitmq.client
            Connection
            ConnectionFactory
            Consumer
            DefaultConsumer
            Channel
            Envelope
            AMQP$BasicProperties]))

(defn adapt
  [{:keys [function] :as handler} channel]
  (proxy [DefaultConsumer] [channel]
    (handleDelivery [tag envelope properties body]
      (function (String. body)))
    (handleCancel [tag])
    (handleCancelOk [tag])))

(defn consume
  ([channel queue autoack tag {:keys [id] :as handler}]
   (.basicConsume channel
                  ^String (name queue)
                  ^Boolean autoack
                  ^String (name id)
                  ^Consumer (adapt handler channel))))

(def ^{:dynamic true
       :doc "Default connection options."}
  *default-options*
  {:username "guest"
   :password "guest"
   :vhost "/"
   :host "localhost"
   :heartbeat ConnectionFactory/DEFAULT_HEARTBEAT
   :timeout ConnectionFactory/DEFAULT_CONNECTION_TIMEOUT
   :port ConnectionFactory/DEFAULT_AMQP_PORT
   :recovery-interval 5000 ;; 5s
   :topology-recovery true})

(defn connect
  [{:keys [host port username password vhost heartbeat timeout
           recovery-interval topology-recovery]
    :as options}]
  (let [cfactory (ConnectionFactory.)]
    (doto cfactory
      (.setUsername ^String username)
      (.setPassword ^String password)
      (.setHost ^String host)
      (.setPort ^long port)
      (.setVirtualHost ^String vhost)
      (.setRequestedHeartbeat ^long heartbeat)
      (.setConnectionTimeout ^long timeout)
      (.setNetworkRecoveryInterval ^long recovery-interval)
      (.setTopologyRecoveryEnabled ^bool topology-recovery))
    (.newConnection cfactory (str "custom:" port))))
