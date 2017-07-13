(ns spirit.httpkit.server.ws-handler
  (:require [hara.component :as component]
            [spirit.common.atom :as atom]
            [spirit.common.http.transport :as transport]
            [org.httpkit.server :as http]
            [clojure.core.async :as async]))

(defn initialise-connection
  ([{:keys [requests channel format handlers connections] :as conn}]
   (doto channel
     (http/on-receive
      (fn [msg]
        (let [{:keys [id type data request response] :as package} (transport/read-value msg format)]
          (case id
            :ch/open (let [{:keys [uuid]} data]
                       (if (and uuid (get @connections uuid))
                         (http/send! channel (transport/write-value
                                              {:id id
                                               :type :response
                                               :response request
                                               :data {:uuid uuid :added false}}
                                              format))
                         
                         (let [uuid (or uuid (str (java.util.UUID/randomUUID)))]
                           (swap! connections assoc uuid conn)
                           (http/send! channel (transport/write-value
                                                {:id id
                                                 :type :response
                                                 :response request
                                                 :data {:uuid uuid :added true}}
                                                format)))))
            
            (case type
              :response (let [ch (get @requests response)]
                          (swap! requests dissoc response)
                          (async/put! ch package))
              
              :request  (if-let [handler (get handlers id)]
                          (http/send! channel
                                      (transport/write-value
                                       {:id   id
                                        :type :response
                                        :response request
                                        :data (handler data)} format))
                          (http/send! channel
                                      (transport/write-value
                                       {:id   id
                                        :type :response
                                        :response request
                                        :data data}
                                       format)))
              (throw (ex-info "NOT IMPLEMENTED")))))))))
  ([conn opts]
   (initialise-connection (merge conn opts))))

(defn wrap-request
  [handler requests]
  (fn [{:keys [return] :as conn} {:keys [request] :as package}]
    (let [request (or request (str (java.util.UUID/randomUUID)))
          ch      (async/promise-chan)]
      (swap! requests assoc request ch)
      (handler conn (assoc package :type :request :request request))
      (case return
        :channel ch
        :value   (async/<!! ch)
        :promise (let [p (promise)]
                   (async/go (deliver p (async/<! ch)))
                   p)))))

(defn base-handler [{:keys [channel format] :as conn} package]
  (http/send! channel (transport/write-value package format)))

(defrecord HttpkitServerWsConnection [channel requests state]

  transport/IConnection
  (-request [conn package]
    ((wrap-request base-handler requests) conn package))
  
  (-push [conn package]
    (base-handler conn package)))

(defn ws-connection
  ([channel]
   (map->HttpkitServerWsConnection
    {:channel  channel
     :requests (atom {})
     :handler  base-handler}))
  ([channel opts]
   (merge (ws-connection channel opts))))

(defn ws-endpoint-handler
  [{:keys [connections] :as ws-endpoint}
   {channel :async-channel :as req}]
  (if (and (:websocket? req)
           (http/send-websocket-handshake! channel req))
    (let [conn (doto (ws-connection channel)
                 (initialise-connection ws-endpoint))]
      {:body channel})
    (throw (ex-info "HTTP not supported" {:uri req}))))

(defrecord HttpkitServerWsEndpoint []

  component/IComponent
  
  (component/-start [endpoint]
    (let [connections (atom {})
          endpoint (assoc endpoint :connections connections)
          handler (partial ws-endpoint-handler endpoint)]
      (assoc endpoint :handler handler)))

  (component/-stop [{:keys [connections] :as endpoint}]
    (doseq [conn @connections]
      (http/close (:channel conn)))
    (dissoc endpoint :handler :connections)))

(defn ws-endpoint
  [m]
  (map->HttpkitServerWsEndpoint m))


(comment

  (def ^:dynamic *ep* (-> (ws-endpoint {:format :edn
                                        :handlers {:ch/open (fn [data] :ch/approved)}})
                          (component/start)))
  
  (component/stop *ep*)
  
  (def close-fn (http/run-server (:handler *ep*) {:port 8080})) 
  
  (close-fn)
  
  (def conn (ws/connect "ws://localhost:8080"
                        :on-error  (fn [error]
                                     (prn error))
                        :on-connect (fn [session]
                                      (ws/send-to-endpoint (str {:id :ch/open
                                                                 :type :request
                                                                 :request (str (java.util.UUID/randomUUID))})
                                                           (.getRemote session)))
                        :on-receive (fn [msg] (println "CLIENT:" (read-string msg)))
                        :on-close  (fn [x y]
                                     (prn x y))))
  
  (ws/close conn)
  


  
  (require '[spirit.jetty.websocket :as ws])
  
  (require '[spirit.httpkit.server :as server])
  (require '[org.httpkit.client :as client])

  @(client/get "http://localhost:8080")
  
  
  (def sys (server/server {}))

  (def sys *1)
  
  {:path     "api"
   :format   :edn
   :handlers {:on/me (fn [req] (prn req) :on/me)}}
  
  )

(comment
  
  (def ^:dynamic *channels*
    (atom {:ready {}
           :open  {}}))

  (require 'lucid.mind)

  (defmethod print-method clojure.core.async.impl.protocols.Channel
    [v w]
    (.write w (str "#chan")))

  (def ch (async/chan))
  
  (async-queues)
  (lucid.mind/.%> clojure.core.async.impl.channels.ManyToManyChannel)
  [clojure.core.async.impl.channels.ManyToManyChannel [java.lang.Object #{clojure.lang.IType clojure.core.async.impl.protocols.Channel clojure.core.async.impl.protocols.WritePort clojure.core.async.impl.protocols.ReadPort clojure.core.async.impl.channels.MMC}]]
  
  

  
  (async/<!! (async/go-loop [seconds 1]
               (async/<! (async/timeout 1000))
               (println "waited" seconds "seconds")
               (if (< seconds 10)
                 (recur (inc seconds)))))
  
  (defn async-handler [req]
    ;; unified API for WebSocket and HTTP long polling/streaming
    (http/with-channel req channel             ; get the channel
      
      (if (go (async/alt! (async/timeout 1000)))
        (async/go-loop ))
      (http/on-close   channel (fn [status]
                                 (prn "SERVER CHANNEL CLOSED:" status channel)))
      (http/on-receive channel (fn [data]    ; two way communication
                                 (let [data (read-string data)]
                                   (http/send! channel (str {:id :ch/open :type :reply :status :error :data (dissoc data :id)})))))))

  (defn async-handler [req]
    (prn (:async-channel req) (:websocket? req)))
  
  
  (ws/send-msg conn "HI")
  (ws/close conn)
 )
