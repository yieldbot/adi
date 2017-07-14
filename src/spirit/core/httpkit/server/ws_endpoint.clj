(ns spirit.core.httpkit.server.ws-endpoint
  (:require [hara.component :as component]
            [org.httpkit.server :as httpkit]
            [spirit.core.httpkit.server.ws-connection :as conn]))

(defn endpoint-handler
  [{:keys [connections] :as endpoint}
   {channel :async-channel :as req}]
  (if (and (:websocket? req)
           (httpkit/send-websocket-handshake! channel req))
    (let [conn (conn/ws-connection channel endpoint)]
      {:body channel})
    (throw (ex-info "HTTP not supported" {:uri req}))))

(defrecord HttpkitServerWsEndpoint []

  Object
  (toString [endpoint]
    (str "#ws" (into {} (-> endpoint
                            (update-in [:registry] (comp count deref))
                            (update-in [:handlers] keys)
                            (dissoc :handler)))))
  
  component/IComponent  
  (component/-start [endpoint]
    (let [registry (atom {})
          endpoint  (assoc endpoint :registry registry)
          handler   (fn [req] (endpoint-handler endpoint req))]
      (assoc endpoint :handler handler)))
  
  (component/-stop [{:keys [registry] :as endpoint}]
    (doseq [[id conn] @registry]
      (httpkit/close (:channel conn)))
    (dissoc endpoint :handler :registry)))

(defmethod print-method HttpkitServerWsEndpoint
  [v w]
  (.write w (str v)))

(defn ws-endpoint
  [m]
  (map->HttpkitServerWsEndpoint m))

(comment

  (require )
  
  
  (def ^:dynamic  (first (vals @(:registry *ep*)))
    (-> (ws-endpoint {:format :edn
                      :handlers {:on/me (fn [data] :hello)}})
        (component/start)))
  
  (def close-fn (httpkit/run-server (:handler *ep*) {:port 8080}))
  
  (close-fn)
  ()

  
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
                          :data {:command :channel/handshake}})))
