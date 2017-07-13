(ns spirit.httpkit.server.ws-endpoint
  (:require [spirit.httpkit.server.ws-connection :as conn]))

(defn endpoint-handler
  [{:keys [connections] :as endpoint}
   {channel :async-channel :as req}]
  (if (and (:websocket? req)
           (http/send-websocket-handshake! channel req))
    (let [conn (conn/ws-connection channel endpoint)]
      {:body channel})
    (throw (ex-info "HTTP not supported" {:uri req}))))

(defrecord HttpkitServerWsEndpoint []

  component/IComponent
  
  (component/-start [endpoint]
    (let [registry (atom {})
          endpoint  (assoc endpoint :registry registry)
          handler   (fn [req] (endpoint-handler endpoint req))]
      (assoc endpoint :handler handler)))
  
  (component/-stop [{:keys [connections] :as endpoint}]
    (doseq [conn @connections]
      (http/close (:channel conn)))
    (dissoc endpoint :handler :connections)))

(defn ws-endpoint
  [m]
  (map->HttpkitServerWsEndpoint m))

(comment

  (require '[org.httpkit.server :as http])
  (require '[spirit.jetty.websocket :as ws])
  
  (def ^:dynamic *ep*
    (-> (ws-endpoint {:format :edn
                      :handlers {:on/me (fn [data] :hello)}})
        (component/start)))
  
  (def close-fn (http/run-server (:handler *ep*) {:port 8080}))
  
  (close-fn)
  
  (def conn (ws/connect "ws://localhost:8080"
                        :on-connect (fn [session]
                                      (ws/send-to-endpoint
                                       (str {:id :channel/handshake
                                             :request (str (java.util.UUID/randomUUID))
                                             :data {:uuid (str (java.util.UUID/randomUUID))}})
                                       (.getRemote session)))
                        :on-receive (fn [msg] (println "CLIENT:" (read-string msg)))
                        :on-close   (fn [x y] (prn x y))))
  
  (ws/send-msg conn (str {:id :channel/echo
                          :request "HELLO"
                          :data {:command :hello}}))
  
  (ws/send-msg conn (str {:id :ws/command
                          :request "HELLO"
                          :data {:command :channel/handshake}}))
  
  )
