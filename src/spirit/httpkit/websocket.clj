(ns spirit.httpkit.websocket
  (:require [org.httpkit.server :as httpkit]
            [hara.component :as component]
            [spirit.httpkit.websocket.connection :as conn]))

(defn websocket-handler
  [{:keys [connections] :as endpoint}
   {raw :async-channel :as req}]
  (if (and (:websocket? req)
           (httpkit/send-websocket-handshake! raw req))
    (let [conn (conn/websocket-connection {:raw raw :endpoint endpoint})]
      {:body raw})
    (throw (ex-info "HTTP not supported" {:uri req}))))

(defrecord HttpkitWebsocket []

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
          handler   (fn [req] (websocket-handler endpoint req))]
      (assoc endpoint :handler handler)))
  
  (component/-stop [{:keys [registry] :as endpoint}]
    (doseq [[id conn] @registry]
      (component/stop conn))
    (dissoc endpoint :handler :registry)))

(defmethod print-method HttpkitWebsocket
  [v w]
  (.write w (str v)))

(defn websocket
  [m]
  (-> (map->HttpkitWebsocket m)
      (component/start)))
