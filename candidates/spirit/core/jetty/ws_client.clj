(ns spirit.jetty.ws-client
  (:require [hara.component :as component]
            [spirit.transport.net :as transport]
            [clojure.core.async :as async])
  (:import  (java.util List UUID)
            (java.net URI)
            (org.eclipse.jetty.util.ssl SslContextFactory)
            (org.eclipse.jetty.websocket.api WebSocketListener
                                             RemoteEndpoint
                                             Session)
            (org.eclipse.jetty.websocket.api.extensions ExtensionConfig)
            (org.eclipse.jetty.websocket.client ClientUpgradeRequest
                                                WebSocketClient)))

(defn set-request-headers
  [^ClientUpgradeRequest request headers]
  (doseq [[header value] headers]
    (let [header-values (if (sequential? value)
                          value
                          [value])]
      (.setHeader
        request
        ^String header
        ^List header-values))))

(defn upgrade-request
  ([]
   (upgrade-request {}))
  ([{:keys [headers subprotocols extensions]}]
   (cond-> (ClientUpgradeRequest.)
     headers (set-request-headers headers)
     subprotocols (.setSubProtocols ^List (into () subprotocols))
     extensions   (.setExtensions ^List (map #(ExtensionConfig. ^String %)
                                             extensions)))))

(defn handle-package
  [{:keys [handlers] :as client}
   {:keys [id type data request] :as package}]
  (if-let [handler (get handlers id)]
    (handler data)
    (throw (ex-info "Handler not found:" package))))

(defn wrap-read-package
  [handler]
  (fn [{:keys [format] :as client} msg]
    (let [package (transport/read-value msg format)]
      (handler client package))))

(defn wrap-response
  [handler]
  (fn [{:keys [requests] :as client}
       {:keys [response] :as package}]
    (if-let [chan (get @requests response)]
      (do (swap! requests dissoc response)
          (async/put! chan (dissoc package :response)))
      (handler client package))))

(defn wrap-return
  [handler]
  (fn [{:keys [state format] :as client}
       {:keys [id request data] :as package}]
    (let [result (handler client package)]
      (if request
        (transport/-send client {:id    id
                                 :response request
                                 :input data
                                 :data  result})))))

(defn request
  [{:keys [return requests] :as comms} {:keys [request] :as package}]
  (prn "REQUEST" package)
  (let [request (or request (str (java.util.UUID/randomUUID)))
        ch      (async/promise-chan)]
    (swap! requests assoc request ch)
    (transport/-send comms (assoc package :type :request :request request))
    (case return
      :channel ch
      :value   (async/<!! ch)
      :promise (let [p (promise)]
                 (async/go (deliver p (async/<! ch)))
                 p))))

(defn raw-listener
  ^WebSocketListener
  [{:keys [handlers state] :as client}]
  (reify WebSocketListener
    (onWebSocketText [_ msg]
      (prn "CLIENT:" msg)
      ((-> handle-package
           wrap-return
           wrap-response
           wrap-read-package) client msg))
    (onWebSocketBinary [_ data offset length]
      (throw (ex-info "Not Implemented" {:method "onWebSocketBinary"})))
    (onWebSocketError [_ throwable]
      (println "ERROR" throwable))
    (onWebSocketConnect [_  session]
      (swap! state assoc :session session))
    (onWebSocketClose [_ x y]
      ((:on-close handlers) client x y))))

(defn raw-client [{:keys [secure]}]
  ^WebSocketClient
  (if secure
    (WebSocketClient. (SslContextFactory.))
    (WebSocketClient.)))

(defrecord JettyWsClient [state]
  Object
  (toString [client]
    (str "#ws.jetty" (into {} client)))
  
  transport/IConnection
  (-request [client package]
    (request client package))
  
  (-send    [{:keys [format state] :as client} package]
    (.sendStringByFuture (.getRemote (:session @state))
                         (transport/write-value package format)))
  
  component/IComponent
  (-start [client])
  (-stop [client]))

(defn ws-client [m]
  (map->JettyWsClient m))

(defn connect
  ([client]
   (connect client {}))
  ([{:keys [protocol host port path url state] :as client} options]
   (let [uri      (URI/create (or url (str protocol "://" host ":" port "/" path)))
         raw      (doto (raw-client {:secure (= "wss" protocol)})
                    (.start))
         req  (upgrade-request options)
         listener (raw-listener client)
         _   @(.connect raw listener uri req)
         data (request (assoc client :return :value) {:id :channel/handshake})]
     (prn "CONNECTED:" data))))

(comment
  
  (def client (doto (ws-client {:protocol "ws"
                                :host "localhost"
                                :port 8080
                                :path "v1"
                                :state (atom {})
                                :requests (atom {})
                                :format :edn
                                :return :value
                                })
                (connect)))
  (:id (:state client))
  (transport/-request (assoc client :return :channel) {:id :channel/echo})
  (transport/-request client {:id :channel/echo})
  (transport/-request (assoc client :return :value) {:id :channel/handshake})
  
  
  
  (connect client)1
  
  )





