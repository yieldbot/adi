(ns spirit.httpkit.client
  (:require [hara.component :as component]
            [org.httpkit.client :as http]
            [spirit.common.http.client.base :as base]
            [spirit.common.http.client :as common]
            [spirit.common.http.transport :as transport]
            [ring.util.codec :as codec]
            [clojure.core.async :as async]))

(defn query-string
  "create a query string from a map
 
   (query-string {})
   => \"\"
   
   (query-string {:timing true})
   => \"?timing=true\""
  {:added "0.5"}
  [params]
  (if-not (empty? params)
    (->> (for [[k v] params]
           (str (codec/url-encode (name k)) "=" (codec/url-encode v)))
         (interpose "&")
         (apply str)
         (str "?"))
    ""))

(defn create-url
  "`create-url` for client
 
   (def cl
     (client/create {:type      :httpkit
                     :protocol  \"http\"
                     :host      \"localhost\"
                     :port      8001
                     :path      \"api\"
                     :routes    {:on/id \"id\"}}))
   
   (create-url cl :on/id {})
   => \"http://localhost:8001/api/id\""
  {:added "0.5"}
  [{:keys [protocol host port routes path] :as client} id params]
  (if-let [route (and routes
                      (get routes id))]
    (str protocol "://" host ":" port "/" (if path (str path "/")) route (query-string params))
    (throw (ex-info "No route found for:" {:id id :client client}))))

(defn http-post
  "httpkit function to client
 
   (def cl
     (client/create {:type      :httpkit
                     :protocol  \"http\"
                     :host      \"localhost\"
                     :port      8001
                     :path      \"api\"
                     :routes    {:on/id \"id\"}}))
   
   (-> @(http-post cl {:id :on/id
                       :params {:timing true}
                       :data {}}
                  {})
       :opts
       :url)
   => \"http://localhost:8001/api/id?timing=true\""
  {:added "0.5"}
  [{:keys [format] :as client}
   {:keys [id params data]}
   {:keys [callback process] :as opts}]
  (let [client (merge client opts)
        url    (create-url client id params)
        result (http/post url {:as :text
                               :body (transport/write-value data format)}
                          callback)]
    (if process
      (process result)
      result)))

(defn wrap-response-errors
  "wrap errors "
  {:added "0.5"}
  [handler]
  (fn [response]
    (let [result (handler response)]
      result)))

(defn wrap-response
  ""
  [handler]
  (fn [client package {:keys [process callback] :as opts}]
    (let [process  (if process  (wrap-response-errors process))
          callback (if callback (wrap-response-errors callback))]
      (handler client package (assoc opts
                                     :process process
                                     :callback callback)))))

(defn wrap-promise
  ""
  [handler]
  (fn [{:keys [format] :as client} package opts]
    (let [p (promise)
          callback (fn [{:keys [body] :as response}]
                     (deliver p (transport/read-body body format)))]
      (handler client package (assoc opts :callback callback))
      p)))

(defn wrap-channel
  ""
  [handler]
  (fn [{:keys [format] :as client} package opts]
    (let [ch  (async/promise-chan)
          callback (fn [{:keys [body] :as response}]
                     (async/put! ch (transport/read-body body format))
                     (async/close! ch))] 
      (handler client package (assoc opts :callback callback))
      ch)))

(defn wrap-value
  ""
  [handler]
  (fn [{:keys [format] :as client} {:keys [id data] :as package} opts]
    (let [result (handler client package {:process deref})]
      (if-let [error (:error result)]
        (assoc package :status :error :error error)
        (-> (:body result)
            (transport/read-body format))))))

(defrecord HttpkitClient [port]
  Object
  (toString [client]
    (str "#httpkit.client" (into {} client)))
  
  transport/IConnection
  (transport/-push    [{:keys [handler] :as client} data opts]
    (handler client data opts))
  
  (transport/-request [{:keys [handler] :as client} data opts]
    (handler client data opts))
  
  component/IComponent
  (component/-start [{:keys [return] :as client}]
    (let [handler (wrap-response http-post)
          handler (case return
                    :promise (wrap-promise handler)
                    :channel (wrap-channel handler)
                    (wrap-value handler))]
      (assoc client :handler handler)))
  
  (component/-stop [client]
    (dissoc client :handler)))

(defmethod print-method HttpkitClient
  [v w]
  (.write w (str v)))

(defmethod base/create :httpkit
  [m]
  (map->HttpkitClient (merge common/*default-config* m)))

(defn httpkit-client
  "" [m]
  (-> (base/create (assoc m :type :httpkit))
      (component/start)))

(comment

  (def client (httpkit-client {:protocol "http"
                               :host     "localhost"
                               :port     8981
                               :format   :edn
                               ;;:return   :channel ;; :promise :data
                               :path     "api"
                               :routes   {:on/id    "id"}}))
  
  (request-channel client {})
  
  (common/request client {:id :on/id})
  
  (component/stop client)
  
  ((:handler client) client
   {:id   :on/id
    :data {}}
   {}))

(comment

  (require '[spirit.httpkit.server :as server])

  (def server (server/server {:port 8981
                              :applications
                              {:default {:path     "api"
                                         :handlers {:on/id (fn [data] :on/id)}
                                         :routes   {:on/id "id"}}}}))
  
  (component/stop server)
  
  (def ch (async/promise-chan))
  
  (async/put! ch 1)
  (async/put! ch 1)
  (async/close! ch)
  
  (async/put! ch nil)
  
  
  

  
  @(http/get "http://localhost:8981/api/ide" {:as :text})
  (httpkit-client {})

  )
