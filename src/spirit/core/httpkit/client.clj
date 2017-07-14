(ns spirit.core.httpkit.client
  (:require [hara.component :as component]
            [spirit.http.client.base :as base]
            [spirit.http.client :as common]
            [spirit.http.transport :as transport]
            [org.httpkit.client :as http]
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
   (def server (new-httpkit-server))
   
   (def client (new-httpkit-client))
 
   (-> (http-post client {:id :on/id})
       deref
       :body
       read-string)
   => {:id :on/id
       :type :reply
       :status :success
       :data {:on/id true}}
   
   (-> (http-post client {:id :on/error})
       deref
       :body
       read-string)
   => {:id :on/error
       :type :reply
       :status :error
       :data {:on/error true}}
 
   (component/stop server)"
  {:added "0.5"}
  [{:keys [format callback] :as client} {:keys [id params data]}]
  (let [url    (create-url client id params)]
    (http/post url {:as :text
                    :body (transport/write-value data format)}
               callback)))

(defn process-response
  "processes the response - either errors or success
   
   (def server (new-httpkit-server))
 
   (def client (new-httpkit-client))
   
   (process-response
    @(http-post client {:id :on/id})
    client
    {:id :on/id})
   => {:data {:on/id true}, :type :reply, :status :success, :id :on/id}
 
   (component/stop server)
   
   (process-response
    @(http-post client {:id :on/id})
    client
    {:id :on/id})
   => (contains-in {:opts {:as :text,
                           :body \"nil\",
                           :method :post,
                           :url \"http://localhost:8001/v1/id\"},
                    :id :on/id,
                    :type :reply,
                    :status :error,
                    :data {:exception java.net.ConnectException},
                    :input nil})"
  {:added "0.5"}
  [{:keys [error body] :as response}
   {:keys [format] :as client}
   {:keys [id data] :as package}]
  (-> (cond error
            (assoc (dissoc response :error)
                   :id id
                   :type :reply
                   :status :error
                   :data {:exception error}
                   :input data)
            
            :else
            (transport/read-body body format))
      (transport/response)))

(defn return-channel
  "the return channel process compatible with core.async
   
   (def server (new-httpkit-server))
 
   (def client (new-httpkit-client))
   
   (async/<!! (return-channel http-post client {:id :on/id}))
   => {:data {:on/id true}, :type :reply, :status :success, :id :on/id}
 
   (component/stop server)
   (async/<!! (return-channel http-post client {:id :on/id}))
   => (contains-in
       {:opts {:as :text, :body \"nil\", :method :post, :url \"http://localhost:8001/v1/id\"},
        :id :on/id,
        :type :reply,
        :status :error,
        :data {:exception java.net.ConnectException},
        :input nil})"
  {:added "0.5"}
  [handler client package]
  (let [ch (async/chan)
        callback  (fn [response]
                    (async/put! ch (process-response response client package))
                    (async/close! ch))]
    (handler (assoc client :callback callback) package)
    ch))

(defn return-promise
  "the return channel process as a promise
 
   (def server (new-httpkit-server))
   
   (def client (new-httpkit-client))
   
   (deref (return-promise http-post client {:id :on/id}))
   => {:id :on/id
       :type :reply
       :status :success 
       :data {:on/id true}}
   
   (component/stop server)
 
   (deref (return-promise http-post client {:id :on/id}))
   => (contains-in
       {:opts {:as :text, :body \"nil\", :method :post, :url \"http://localhost:8001/v1/id\"},
        :id :on/id,
        :type :reply,
        :status :error,
        :data {:exception java.net.ConnectException},
        :input nil})"
  {:added "0.5"}
  [handler client package]
  (let [p (promise)
        callback  (fn [response]
                    (deliver p (process-response response client package)))]
    (handler (assoc client :callback callback) package)
    p))

(defn wrap-return
  "returns the required interface depending on `:return` value
 
   (def server (new-httpkit-server))
   
   (def client (new-httpkit-client))
   
   ((wrap-return http-post)
    (assoc client :return :value)
    {:id :on/id})
   => {:id :on/id
       :type :reply
       :status :success
       :data {:on/id true}}
 
   (-> ((wrap-return http-post)
        (assoc client :return :channel)
        {:id :on/id})
       (async/<!!))
   => {:id :on/id
       :type :reply
       :status :success
       :data {:on/id true}}
 
   (-> ((wrap-return http-post)
        (assoc client :return :promise)
        {:id :on/id})
       (deref))
   => {:id :on/id
       :type :reply
       :status :success
       :data {:on/id true}}
 
   (component/stop server)"
  {:added "0.5"}
  [handler]
  (fn [{:keys [return] :as client} package]
    (case return
      :promise (return-promise handler client package)
      :channel (return-channel handler client package)
      :value   (deref (return-promise handler client package)))))

(defrecord HttpkitClient [port]
  Object
  (toString [client]
    (str "#httpkit.client" (into {} client)))
  
  transport/IConnection
  (transport/-push    [{:keys [handler] :as client} data]
    (handler client data))
  
  (transport/-request [{:keys [handler] :as client} data]
    (handler client data))
  
  component/IComponent
  (component/-start [{:keys [return] :as client}]
    (let [handler (wrap-return http-post)]
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
  "creates a httpkit client for http transport
 
   (def server (new-httpkit-server))
   
   (-> (httpkit-client {:port     8001
                        :return   :value ;; :promise :data
                        :path     \"v1\"
                        :routes   {:on/id \"id\"
                                   :on/error \"error\"}})
       (client/request {:id :on/id}))
   => {:id :on/id
       :type :reply
       :status :success
       :data {:on/id true}}
   
   (component/stop server)"
  {:added "0.5"}
  [m]
  (-> (base/create (assoc m :type :httpkit))
      (component/start)))
