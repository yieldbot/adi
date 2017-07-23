(ns spirit.transport.base
  (:require [spirit.protocol.itransport :as transport]
            [clojure.core.async :as async]
            [hara.component :as component]
            [hara.event :as event]))

(defn deliver-response
  [{:keys [requests return] :as server}
   {:keys [id response data] :as package}]
  (if-let [ch (get @requests response)]
    (do (async/put! ch (dissoc package :response))
        (async/close! ch)
        (swap! requests dissoc response))
    (event/raise {:id id :data data :class #{:http/no-request-found}})))

(defn deliver-wrap-delay [handler]
  (fn [service {:keys [params] :as package}]
    (let [_     (if-let [delay (-> params :delay :send)]
                  (Thread/sleep delay))
          result (handler service package)
          _      (if-let [delay (-> params :delay :receive)]
                   (Thread/sleep delay))]
      result)))

(defn deliver-wrap-request [handler]
  (fn [{:keys [requests return] :as service}
       {:keys [request] :as package}]
    (let [request (or request (str (java.util.UUID/randomUUID)))
        ch      (async/chan)
        _       (swap! requests assoc request ch)
        result  (handler service (assoc package :type :request :request request))
        _       (deliver-response service result)]
    (case return
      :channel ch
      :value   (async/<!! ch)))))

(defn deliver-wrap-send [handler]
  (fn [service package]
    (handler service package)))

(defn process-request
  [server
   {:keys [request] :as package}
   handler]
  (let [result (handler server package)]
    (-> result
        (assoc :type :response
               :response request))))

(defn process-wrap-delay-handler
  [handler]
  (fn [service {:keys [params] :as package}]
    (if-let [delay (->  params :delay :handler)]
      (Thread/sleep delay))
    (handler service package)))

(defn process-wrap-time-handler
  [handler]
  (fn [service {:keys [params] :as package}]
    (cond (-> params :time :handler)
          (let [start  (System/nanoTime)
                result (handler service package)
                end    (System/nanoTime)]
            (assoc-in result [:meta :time :handler] (quot (- end start) 1000000)))
          
          :else
          (handler service package))))

(defn process-wrap-type
  [handler]
  (fn [server {:keys [type] :as package}]
    (case type
      :response (deliver-response server package)
      :request  (process-request server package handler)
      (handler server package))))

(defn process-base [{:keys [handlers] :as server}
                    {:keys [id data] :as package}]
  (let [handler (or (get handlers id)
                    (event/raise {:id id :data data :class #{:http/no-handler-found}}))
        result  (handler data)]
    {:id id :data result}))

(defrecord MockSingleton [handler]

  Object
  (toString [service]
    (str "#singleton" (-> (:handlers service) keys sort vec)))

  transport/IConnection
  (-request [service package]
    ((-> (:handler service)
         deliver-wrap-delay
         deliver-wrap-request) service package))
  
  (-send    [service package]
    ((deliver-wrap-send (:handler service)) service package))
  
  component/IComponent
  (-start [service]
    (assoc service
           :requests (atom {})
           :handler  (-> process-base
                         process-wrap-delay-handler
                         process-wrap-time-handler
                         process-wrap-type)))

  (-stop [service]
    (dissoc service :handler :requests)))

(defmethod print-method MockSingleton
  [v w]
  (.write w (str v)))

(defn singleton [m]
  (-> (map->MockSingleton m)
      (component/start)))



(comment


  (def sys (singleton {:return :value
                       :handlers {:on/id (fn [data] :id/zcaudate)}}))
  
  (time (transport/-request sys {:id :on/id
                                 :params {:delay {:send 100
                                                  :receive 100
                                                  :handler 200}
                                          :time  {:handler true}}}))
  
  (transport/-send sys {:id :on/id})
  
  )
  
  
