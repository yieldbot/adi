(ns spirit.network.base.hub
  (:require [spirit.network.common :as common]
            [spirit.network.base.endpoint :as endpoint]
            [hara.component :as component]))

(defrecord Hub []

  Object
  (toString [conn]
    (str "#net.hub" (into {}
                          (-> conn
                              (update-in [:handlers] keys)
                              (update-in [:connections] (fnil (comp keys deref) (atom {})))))))
  
  component/IComponent
  (-start [conn]
    (assoc conn :connections (atom {})))
  (-stop [conn]
    (dissoc conn :connections)))

(defmethod print-method Hub
  [v w]
  (.write w (str v)))

(defn hub
  "creates a hub in order to connect multiple endpoints to
 
   (def sys (hub {:format :edn
                 :options {:time  true
                            :track true}
                  :default {:params {:full true
                                     :metrics true}}
                  :return  {:type    :channel
                            :timeout 1000}
                  :handlers  {:on/id (fn [req] :a)}}))"
  {:added "0.5"}
  [m]
  (-> (map->Hub m)
      (component/start)))

(defn connect
  "returns an endpoint for connection
 
   (def ca (connect sys))
   
   (common/request ca :on/id nil {:params {}})
   => :a"
  {:added "0.5"}
  [hub]
  (let [hide     [:hide :fn :raw :hub :handlers :state :pending]
        config   (into {} (dissoc hub :connections))
        internal (endpoint/endpoint
                  (assoc config
                         :hide hide
                         :fn {:close   (fn [{:keys [hub id] :as conn}]
                                         (swap! (:connections hub) dissoc id)
                                         (endpoint/close-fn conn))}
                         :hub hub))
        external (endpoint/endpoint
                  (-> config
                      (dissoc :handlers)
                      (assoc :hide hide)))]
    (endpoint/connect internal external)
    (swap! (:connections hub) assoc (:id internal) internal)
    external))

(defn list-connections
  "returns all active connections
 
   (list-connections sys)
   ;;=> (\"9879eab5-b78f-4bfc-b341-08c9a80b6ce5\"
   ;;    \"fa07e87f-0c34-4618-9a96-1d43b930f7c8\")
   "
  {:added "0.5"}
  [{:keys [connections] :as hub}]
  (keys @connections))

(defn close-connection
  "returns all active connections
 
   (close-connection sys \"9879eab5-b78f-4bfc-b341-08c9a80b6ce5\")"
  {:added "0.5"}
  [{:keys [connections] :as hub} id]
  (when-let [conn (get @connections id)]
    (component/stop conn)
    (swap! connections dissoc id))
  hub)
