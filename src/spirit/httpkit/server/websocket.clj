(ns spirit.httpkit.server.websocket
  (:require [hara.component :as component]
            [org.httpkit.server :as http]))

(defrecord HttpKitServerWebsocket []

  component/IComponent
  
  (component/-start [{:keys [packer handler] :as ws}]
    )

  (component/-stop [{:keys [ch-recv stop-fn] :as ws}]
    )
  
  (component/-started? [ws]
    )
  
  (component/-stopped? [ws]
    ))

(defn routes [])

(defn websocket
  [{:keys [packer handler] :as opts}]
  (-> (map->Websocket opts)
      (component/start)))
