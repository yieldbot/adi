(ns spirit.httpkit.server.ws-connection
  (:require [hara.component :as component]
            [spirit.common.atom :as atom]
            [spirit.common.http.transport :as transport]
            [org.httpkit.server :as http]
            [clojure.core.async :as async]))



(defn wrap-close [handler {:keys [channel format registry connection-id] :as conn}])

(defn wrap-open  [handler {:keys [channel format registry connection-id] :as conn}]
  (fn [{:keys [id type data request response] :as package}]
    (if (= :ch/open id)
      (let [{:keys [uuid]} data]
        (if (and uuid (get @registry uuid))
          (http/send! channel (transport/write-value
                               {:id id
                                :type :response
                                :response request
                                :data {:uuid uuid :added false}}
                               format))
          
          (let [uuid (or uuid (str (java.util.UUID/randomUUID)))]
            (swap! registry assoc uuid conn)
            (http/send! channel (transport/write-value
                                 {:id id
                                  :type :response
                                  :response request
                                  :data {:uuid uuid :added true}}
                                 format)))))
      (handler package))))

(defn base-handler [{:keys [id type data request response] :as package}
                    {:keys [channel format requests handlers] :as conn}]
  (case type
    :response (let [ch (get @requests response)]
                (swap! requests dissoc response)
                (async/put! ch package))
    
    :request  (if-let [handler (get handlers id)]
                (http/send! channel
                            (transport/write-value
                             {:id   id
                              :type :response
                              :response request
                              :data (handler data)} format))
                (http/send! channel
                            (transport/write-value
                             {:id   id
                              :type :response
                              :response request
                              :data data}
                             format)))
    (throw (ex-info "NOT IMPLEMENTED"))))

(defn initialise-connection
  ([{:keys [channel] :as conn}]
   (doto channel
     (http/on-receive
      (-> #(base-handler % conn)
          (wrap-ch-open conn)
          (wrap-read-value format))))
   conn)
  ([conn opts]
   (initialise-connection (merge conn opts))))

(defn wrap-request
  [handler requests]
  (fn [{:keys [return] :as conn} {:keys [request] :as package}]
    (let [request (or request (str (java.util.UUID/randomUUID)))
          ch      (async/promise-chan)]
      (swap! requests assoc request ch)
      (handler conn (assoc package :type :request :request request))
      (case return
        :channel ch
        :value   (async/<!! ch)
        :promise (let [p (promise)]
                   (async/go (deliver p (async/<! ch)))
                   p)))))

(defn base-handler [{:keys [channel format] :as conn} package]
  (http/send! channel (transport/write-value package format)))

(defrecord HttpkitServerWsConnection [channel requests registry state]

  transport/IConnection
  (-request [conn package]
    ((wrap-request base-handler requests) conn package))
  
  (-push [conn package]
    (base-handler conn package)))

(defn ws-connection
  ([channel]
   (map->HttpkitServerWsConnection
    {:channel  channel
     :requests (atom {})
     :handler  base-handler}))
  ([channel opts]
   (merge (ws-connection channel opts))))


