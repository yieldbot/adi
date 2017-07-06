(ns spirit.httpkit.server
  (:require [hara.component :as component]
            [org.httpkit.server :as http-kit]
            [spirit.common.http.server :as common]))

(defrecord HttpKitServer [port]
  component/IComponent
  
  (component/-start [{:keys [port routes] :as server}]
    (let [stop-fn (http-kit/run-server routes server)]
      (assoc server :stop-fn stop-fn)))

  (component/-started? [server]
    (boolean (:stop-fn server)))
  
  (component/-stop [server]
    (if-let [stop (:stop-fn server)]
      (stop))
    (dissoc server :stop-fn)))

(defmethod common/create :http-kit
  [m]
  (map->HttpKitServer m))

(defn server [{:keys [port routes] :as opts}]
  (-> (common/create (assoc opts :type :http-kit))
      (component/start)))
