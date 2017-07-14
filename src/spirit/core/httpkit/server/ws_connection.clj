(ns spirit.core.httpkit.server.ws-connection
  (:require [clojure.core.async :as async]
            [org.httpkit.server :as httpkit]
            [spirit.http.transport :as transport]))

(def commands #{:channel/handshake
                :channel/heartbeat
                :channel/get-id
                :channel/set-id
                :channel/close
                :channel/info
                :channel/echo
                :endpoint/ping
                :endpoint/pong
                :endpoint/all-ids})

(defmulti process-command
  (fn [conn {:keys [id] :as package}] id))

(defmethod process-command :channel/handshake
  [{:keys [channel endpoint state] :as conn}
   {:keys [data] :as package}]
  (let [uuid (or (:uuid data) (str (java.util.UUID/randomUUID)))]
    (swap! state assoc :uuid uuid)
    (swap! (:registry endpoint) assoc uuid conn)
    {:uuid uuid}))

(defmethod process-command :channel/heartbeat
  [{:keys [channel endpoint] :as conn}
   _]
  {:health :good})

(defmethod process-command :channel/echo
  [{:keys [channel endpoint] :as conn}
   data]
  data)

(defmethod process-command :channel/info
  [{:keys [channel endpoint] :as conn}
   _]
  {:health :good})

(defn wrap-command
  [handler]
  (fn [conn {:keys [id] :as package}]
    (if (commands id)
      (process-command conn package)
      (handler conn package))))

(defn handle-package
  [{:keys [channel endpoint] :as conn}
   {:keys [id type data request] :as package}]
  (if-let [handler (get (:handlers endpoint) id)]
    (handler data)
    (throw (ex-info "Handler not found:" package))))

(defn wrap-read-package
  [handler]
  (fn [{:keys [endpoint] :as conn} msg]
    (let [package (transport/read-value msg (:format endpoint))]
      (handler conn package))))

(defn wrap-response
  [handler]
  (fn [{:keys [requests] :as conn}
       {:keys [response] :as package}]
    (if-let [chan (get @requests response)]
      (do (swap! requests dissoc response)
          (async/put! chan package))
      (handler conn package))))

;;(def process-command 1)

(defn wrap-return
  [handler]
  (fn [{:keys [channel endpoint] :as conn}
       {:keys [id request data] :as package}]
    (let [result (handler conn package)]
      (if request
        (httpkit/send! channel
                       (transport/write-value
                        {:id    id
                         :response request
                         :input data
                         :data  result} (:format endpoint)))))))

(def connection-handler
  (-> handle-package
      wrap-command
      wrap-return
      wrap-response
      wrap-read-package))

(defn initialise-connection 
  [{:keys [channel] :as conn}]
  (doto channel
    (httpkit/on-receive
     (fn [msg] (connection-handler conn msg))))
  conn)

(defrecord HttpkitServerWsConnection [channel requests endpoint]

  Object
  (toString [conn]
    (str "#ws" (into {} (dissoc conn :endpoint))))
  
  transport/IConnection
  (-request [conn package]
    )
  
  (-send [conn package]
    ))

(defmethod print-method HttpkitServerWsConnection
  [v w]
  (.write w (str v)))

(defn ws-connection
  [channel endpoint]
  (doto (map->HttpkitServerWsConnection
         {:state    (atom {:uuid :null})
          :requests (atom {})
          :channel  channel
          :endpoint endpoint})
    (initialise-connection)))


(comment
  
  (request conn {:id   :channel/set-id
                 :data {:id "<NEWID>"}})
  
  (request conn {:id :channel/get-id}))


(comment
  {:id :channel/get-id
   :request ""}
  ;; => :not-set or 'string'
  
  {:id   :channel/set-id
   :data {:uuid "hello"}}
  ;; => 

  {:id   :channel/ping
   :data {:uuid "hello"}}

  {:id   :endpoint/get-uuid
   :data {:uuid "hello"}}
  
  {:id   :endpoint/list-uuids})
