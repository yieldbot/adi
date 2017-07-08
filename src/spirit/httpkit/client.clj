(ns spirit.httpkit.client
  (:require [hara.component :as component]
            [org.httpkit.client :as http]
            [spirit.common.http.client.base :as base]
            [spirit.common.http.transport :as transport]))

(defrecord HttpKitClient [port]
  Object
  (toString [conn]
    (str "#httpkit.client" (into {} conn)))
  
  transport/IConnection
  (transport/-push   [conn data opts])
  (transport/-request [conn data opts])
  
  component/IComponent
  (component/-start [{:keys [host port] :as client}]
    server)
  
  (component/-started? [server])
  (component/-stop [server]
    server))

(defmethod print-method HttpKitClient
  [v w]
  (.write w (str v)))

(defmethod base/create :httpkit
  [m]
  (map->HttpKitClient m))

(defn httpkit-client [m]
  (-> (base/create (assoc m :type :httpkit))
      (component/start)))
