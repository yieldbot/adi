(ns spirit.http.websocket
  (:require [hara.component :as component]
            [clojure.set :as set]
            [clojure.core.async :as async]
            [taoensso.sente.server-adapters.http-kit :as sente-http]
            [taoensso.sente :as sente]))

(defrecord Websocket []

  component/IComponent
  
  (component/-start [{:keys [packer handler] :as ws}]
    (let [{:keys [ch-recv] :as socket}
          (sente/make-channel-socket! sente-http/http-kit-adapter
                                      (select-keys ws [:packer]))
          stop-fn (sente/start-chsk-router! ch-recv handler)]
      (-> ws
          (assoc :stop-fn stop-fn)
          (merge socket))))
  
  (component/-started? [ws]
    (set/superset? (keys ws)
                   #{:stop-fn :ch-recv}))
  
  (component/-stop [{:keys [ch-recv stop-fn] :as ws}]
    (do (async/close! ch-recv)
        (stop-fn)
        (select-keys ws [:packer :handler])))
  
  (component/-stopped? [ws]
    (not (component/-started? ws))))

(defn websocket
  [{:keys [packer handler] :as opts}]
  (-> (map->Websocket opts)
      (component/start)))

(defn send! [{:keys [send-fn] :as ws} user-id event]
  (send-fn user-id event))

(defn broadcast! [{:keys [connected-uids] :as ws} event]
  (doseq [uid (:any @connected-uids)]
    (send! ws uid event)))
