(ns spirit.transport.server
  (:require [hara.component :as component]
            [org.httpkit.server :as http-kit]))

(defn open-browser [{:keys [protocol host port]
                     :or {host "localhost"
                          protocol "http"}}]
  (let [uri (format "%s://%s:%s" protocol host (str port))]
    (try
      (.browse (java.awt.Desktop/getDesktop) (java.net.URI. uri))
      (catch java.awt.HeadlessException _))))

(defrecord Server [port]
  component/IComponent
  
  (component/-start [{:keys [port options handler wrappers] :as server}]
    (let [stop-fn (http-kit/run-server (reduce (fn [out func]
                                                 (func out))
                                               handler
                                               wrappers)
                                       {:port port})
          _ (if (:open-browser options) (open-browser server))]
      (assoc server :stop-fn stop-fn)))

  (component/-started? [server]
    (boolean (:stop-fn server)))
  
  (component/-stop [server]
    (if-let [stop (:stop-fn server)]
      (stop))
    (dissoc server :stop-fn)))

(defn server [{:keys [port options handler wrappers] :as opts}]
  (-> (map->Server opts)
      (component/start)))
