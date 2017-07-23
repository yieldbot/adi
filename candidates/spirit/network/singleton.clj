(ns spirit.network.singleton
  (:require [hara.component :as component]
            [hara.event :as event]
            [spirit.protocol.itransport :as transport]
            [spirit.network.common :as common]
            [clojure.core.async :as async]))

(comment
  
  "client request workflow"

  <full request>
  
  {:id :on/id
   :params  {:delay {:request  {:send    50
                                :receive 50}
                     :response {:send    50
                                :receive 50}}
             :tag   {}
             :time  {
                     :request {:total   true
                               :remote  true
                               :handler true}}}
   :metrics {:time {:total   {:start 0 :end 0 :duration 0}
                    :remote  {:start 0 :end 0 :duration 0}
                    :handler {:start 0 :end 0 :duration 0}}}
   :data nil}

  <short request>

  {:id :on/id}                ;; add [:total :start] to :metrics
  
  <adds type and request id>  ;; create entry on the :pending table

  {:id :on/id
   :type :request
   :request <ID>}             ;;  delay for [:request :send]
  
  <pack as string and send over network>

  <....   network   ....>     ;; delay for [:network :request]
  
  <unpack as string>          ;; add [:remote :start] time to :metrics

  {:id :on/id
   :type :request            
   :request <ID>}             ;; delay for [:request :receive]
  
  <handle packet based on id> ;; add [:handler :start] to :metrics

  <handle ex-info>            ;; if error, make sure to format it properly
  
  <if request package result as response> ;; add [:handler :end] to :metrics

  <>                          ;; delay for [:response :send]

  <>                          ;; add [:remote :end] to :metrics

  <pack as string as send over network>

  <....   network   ....>    ;; delay for [:network :response]

  <unpack as string>         ;; delay for [:response :receive]
  
  <find request and put result in channel>  ;; add [:total :end] to :metrics
  )

(defn wrap-delay [handler path]
  (fn [conn package]
    (if-let [delay (get-in package path)]
      (Thread/sleep delay))
    (handler conn package)))

(defn wrap-time [handler {:keys [in out]}]
  (fn [conn package]
    (let [package (if (get-in package in)
                    (assoc-in package out (System/currentTimeMillis))
                    package)]
      (handler conn package))))

(defmulti read-value  (fn [string format] format))
(defmulti write-value (fn [data format] format))

(defmethod read-value :edn
  [string _]
  (read-string string))

(defmethod write-value :edn
  [data _]
  (pr-str data))

(defn wrap-transport [handler format method]
  (fn [conn package]
    (let [message (case method
                    :pack   (let [{:keys [id type status params data]} package]
                              (try
                                (write-value package format)
                                (catch Exception e
                                  (write-value {:id id
                                                :type   :error
                                                :input  {:type type
                                                         :data data
                                                         :params params}
                                                :data   {:message (.getMessage e)}} format))))
                    :unpack (try
                              (read-value package format)
                              (catch Exception e
                                {:id     :error/tansport
                                 :type   :error
                                 :input  {:string package}
                                 :data   {:message (.getMessage e)}} format)))]
      (handler conn message))))

(defn send-request-build [{:keys [] :as conn} package])

(defn send-request-deliver [{:keys [raw send-raw] :as conn} package]
  (send-raw raw))

(defn send-base
  [{:keys [raw] :as conn} package]
  (prn "SEND:" package)
  (let [message (common/pack conn package)]
    (send-off raw (fn [_]
                    (if-let [delay (get-in conn [:options :delay :network])]
                      (Thread/sleep delay))
                    message))))

(defn process-base [{:keys [handlers] :as conn}
                    {:keys [id] :as package}]
  (prn "PROCESS:" package)
  ((get handlers id) package))

(defn return-base 
  [{:keys [pending] :as server}
   {:keys [id tag data] :as package}]
  (if-let [ch (get @pending tag)]
    (do (async/put! ch package)
        (async/close! ch)
        (swap! pending dissoc tag))
    (event/raise {:id id :data data :class #{:http/no-request-found}})))

(defn receive-base [{:keys [fn] :as conn} {:keys [type] :as package}]
  (case type
    :request  ((:respond fn) conn package)
    :response ((:return fn) conn package)
    ((:process fn) conn package)))

(defn wrap-unpack [handler]
  (fn [conn message]
    (let [package (common/unpack conn message)]
      (cond (= (:id package) :error/tansport)
            (throw (Exception. "TODO"))

            :else
            (handler conn package)))))

(defn wrap-request [handler]
  (fn [{:keys [pending return] :as conn}
       {:keys [tag] :as package}]
    (let [tag (or tag (str (java.util.UUID/randomUUID)))
          ch        (async/chan)
          _         (swap! pending assoc tag ch)
          result    (handler conn (assoc package :type :request :tag tag))]
      (case return
        :channel ch
        :value   (async/<!! ch)))))

(defn wrap-response [handler]
  (fn [{:keys [fn] :as conn} {:keys [id data tag] :as package}]
    (let [result (handler conn package)]
      (prn "HANDLER RESULT:" result)
      ((-> fn :send) conn {:id id :type :response :tag tag :data result}))))

{:type      :on/use 
 :code      :response ;; broadcast, push, request, send, response, error
 ;;:status   :success
 :tag       "hello"
 :log       []
 :error     {}
 :request   {}
 :response  {}
 :send      {}}

(defn init-singleton [{:keys [format] :as conn}]
  (let [pending (atom {})
        raw (agent nil)
        send-fn    (-> send-base
                       ;;(wrap-transport format :pack)
                       #_(wrap-delay [:params :delay :request :send])
                       #_(wrap-time  {:before {:in  [:params :time :total]
                                             :out [:metrics :time :total :start]}}))
        
        request-fn (-> send-fn
                       (wrap-request))
        
        process-fn (-> process-base
                       #_(wrap-time  {:before {:in  [:params :time :handler]
                                             :out [:metrics :time :handler :start]}
                                    :after  {:in  [:params :time :handler]
                                             :out [:metrics :time :handler :end]}}))
        
        respond-fn (-> process-fn
                       (wrap-response))
        
        return-fn  (-> return-base
                       ;;(wrap-transport format :pack)
                       #_(wrap-delay {:before [:params :delay :response :receive]})
                       #_(wrap-time  {:before {:in  [:params :time :total]
                                             :out [:metrics :time :total :end]}})
                       )
        
        receive-fn (-> receive-base
                       #_(wrap-delay {:before [:params :delay :request :receive]})
                       #_(wrap-time  {:before {:in  [:params :time :remote]
                                             :out [:metrics :time :remote :start]}})
                       (wrap-unpack) ;;(wrap-transport format :unpack)
                       )
        conn        (assoc conn
                           :raw raw
                           :pending pending
                           :fn {:send    send-fn
                                :receive receive-fn
                                
                                :request request-fn
                                :respond respond-fn
                                :return  return-fn
                                :process process-fn
                                })
        _ (add-watch raw :receive-fn (fn [_ _ _ message]
                                       (receive-fn conn message)))]
    conn))


(defrecord Singleton []

  Object
  (toString [conn]
    (str "#net.singleton" (into {}
                                (-> conn
                                    (dissoc :fn :raw)
                                    (update-in [:pending] (fnil deref (atom {})))))))
  
  component/IComponent
  (-start [conn]
    (init-singleton conn))
  (-stop [conn]
    (dissoc conn :pending :raw :fn)))

(defmethod print-method Singleton
  [v w]
  (.write w (str v)))

(defn singleton [m]
  (-> (map->Singleton m)
      (component/start)))

(comment

  {:pending (atom {})
   :options {:delay {:network 10}}
   :raw (agent nil)
   :send-raw send-raw}
  
  (defn send-request-deliver [{:keys [connection send-raw] :as conn} message]
    (send-off (agent 2) inc))
  
  (-> send-request-deliver
      (wrap-transport :edn :pack))

  (-> send-request-build
      (wrap-time  [:params :time :total]
                  [:metrics :time :total :start])
      (wrap-delay [:params :delay :request :send]))


  (defn send-request [conn package]
    (let [package ((-> send-request-build
                       (wrap-time  [:params :time :total]
                                   [:metrics :time :total :start])
                       (wrap-delay [:params :delay :request :send]))
                   conn package)])))
