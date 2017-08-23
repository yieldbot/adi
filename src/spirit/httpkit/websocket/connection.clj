(ns spirit.httpkit.websocket.connection  
  (:require [spirit.network.common :as common]
            [clojure.core.async :as async]
            [org.httpkit.server :as httpkit]
            [hara.component :as component]))

(defn ws-close [{:keys [raw] :as conn} _]
  (component/stop conn))

(defn ws-ping [{:keys [fn] :as conn} _]
  ((:send fn) conn {:type :ws/pong}))

(defn ws-pong [{:keys [fn] :as conn} _])

(def ^:dynamic *commands*
  {:ws/close ws-close
   :ws/ping  ws-ping
   :ws/pong  ws-pong})

(defn send-fn [{:keys [raw options] :as conn} package]
  (let [message (common/pack conn package)]
    (httpkit/send! raw message)
    conn))

(defn attach-fn [{:keys [raw fn] :as conn}]
  (httpkit/on-receive raw (:receive fn))
  (httpkit/on-close raw (:close fn))
  conn)

(defn close-fn [{:keys [raw] :as conn}]
  (httpkit/close raw)
  conn)

(defn active?-fn [{:keys [raw] :as conn}]
  (httpkit/open? raw))

(defrecord HttpkitWebsocketConnection []
  
  Object
  (toString [conn]
    (str "#httpkit.websocket" (into {} (apply dissoc conn (:hide conn)))))
  
  component/IComponent
  (-start [{:keys [hide raw] :as conn}]
    (-> conn
        (assoc :hide (or hide [:fn :raw :handlers :pending :hide])
               :raw  (or raw (atom nil)))
        (update-in [:fn] (partial merge
                                  {:active? active?-fn
                                   :attach  attach-fn
                                   :close   close-fn
                                   :send    send-fn}))
        (common/init-functions)))
  
  (-stop [{:keys [raw hub fn] :as conn}]
    ((:close fn) conn)
    (dissoc conn :pending :fn)))
    
(defn websocket-connection [{:keys [endpoint raw] :as m}]
  (-> (map->HttpkitWebsocketConnection m)
      (component/start)))
