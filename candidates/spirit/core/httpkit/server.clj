(ns spirit.core.httpkit.server
  (:require [org.httpkit.server :as httpkit]
            [spirit.transport.server :as common]
            [hara.component :as component]
            [hara.data.nested :as nested]))

(defrecord HttpkitServer [port]
  Object
  (toString [conn]
    (str "#httpkit.server" (into {} conn)))
  
  component/IComponent
  
  (component/-start [{:keys [port handler applications] :as server}]
    (let [handler (or handler
                      (common/create-handler server applications))
          stop-fn (httpkit/run-server handler server)]
      (assoc server :stop-fn stop-fn)))
  
  (component/-started? [server]
    (boolean (:stop-fn server)))
  
  (component/-stop [server]
    (if-let [stop (:stop-fn server)]
      (stop))
    (dissoc server :stop-fn)))

(defmethod print-method HttpkitServer
  [v w]
  (.write w (str v)))

(defmethod common/create :httpkit
  [m]
  (map->HttpkitServer (nested/merge-nested common/*default-config* m)))

(defn server
  "creating httpkit server
 
   (def sys (server {:handler (fn [_] {:status 200 :body \"hello world\"})}))
 
   (-> @(client/get \"http://localhost:8000\" {:as :text})
       :body)
   => \"hello world\"
 
   (component/stop sys)"
  {:added "0.5"}
  [{:keys [protocol host port enable applications handler] :as opts}]
  (-> (common/create (assoc opts :type :httpkit))
      (component/start)))
