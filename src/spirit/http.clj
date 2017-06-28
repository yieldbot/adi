(ns spirit.http
  (:require [spirit.http
             [server :as server]
             [websocket :as websocket]]
            [hara.component :as component]
            [compojure.core :as http]
            [compojure.route :as route]
            [bidi.bidi :as bidi]
            [bidi.ring :as ring]
            [ring.util.response :as response]
            [ring.middleware
             [defaults :as defaults]
             [params :as params]
             [keyword-params :as keyword-params]
             [nested-params :as nested-params]
             [resource :as resource]]
            [ring.util.mime-type :as mime]))

(defn default-event-handler [{:keys [id uid ?data scope] :as msg}]
  (println "EVENT RECEIVED:" id uid ?data scope)
  msg)

(defn default-request-handler [{:keys [id uid ?data scope] :as msg}]
  (println "REQUEST RECEIVED:" id ?data scope)
  {:id id :uid uid :scope scope :data {:server-echo-back ?data}})

(defn wrap-scope [handler key]
  (fn [m]
    (handler (assoc m :scope key))))

(defn create-websocket-routes
  [ws path]
  (http/routes (http/GET  path
                          req
                          ((:ajax-get-or-ws-handshake-fn ws) req))
               (http/POST path
                          req
                     ((:ajax-post-fn ws) req))))

(defn create-websocket-handler [handlers scope]
  (let [event-handlers   (->> scope
                              (map (fn [k]
                                     (wrap-scope (or (get-in handlers [k :event-handler])
                                                     default-event-handler)
                                                 k))))
        request-handlers (->> scope
                              (map (fn [k]
                                     (wrap-scope (or (get-in handlers [k :request-handler])
                                                     default-request-handler)
                                                 k))))]    
    (fn [{:as msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
      (if ?reply-fn
        (?reply-fn [id (some #(%1 msg) request-handlers)])
        (some #(%1 msg) event-handlers)))))

(defn create-websocket [{:keys [packer path scope handlers]}]
  (let [ws (websocket/websocket {:packer (or packer :edn)
                                 :handler (create-websocket-handler handlers scope)})
        http-routes (-> (create-websocket-routes ws path)
                        )]
    {:ws ws :ws-routes http-routes}))

(defn create-application-handler [path handler routes]
  (fn [{:keys [uri body params] :as request}]
    (let [path (if (.startsWith path "/") path (str "/" path))
          path (if (.endsWith path "/") path (str path "/"))
          result (bidi/match-route [path routes] uri)]
      (if result
        {:status 200
         :headers {"Content-Type" "text/plain"}
         :body (prn-str (handler (assoc request
                                        :id (:handler result)
                                        :?data (merge (:route-params result)
                                                      (cond (string? body)
                                                            (read-string body))
                                                      params))))}))))

(defn create-application [{:keys [packer path scope handlers]}]
    (let [request-handlers (->> scope
                                (map (fn [k]
                                       (-> (create-application-handler
                                            path
                                            (or (get-in handlers [k :handler])
                                                default-request-handler)
                                            (or (get-in handlers [k :routes])
                                                {}))
                                           (wrap-scope k)))))]
      (fn [request]
        (some #(%1 request) request-handlers))))

(defn create-routes
  [ws-routes app-routes resources files]
  (->> [ws-routes
        app-routes
        (if-let [{:keys [path]} resources]
          (route/resources (or path "/")))
        (if-let [{:keys [path root]} files]
          (route/files (or path "/") {:root (or root "resources/public")}))
        (route/not-found "<h1>Page not found</h1>")]
       (keep identity)
       (apply http/routes)))

(defn wrap-app [handler]
  (let [config (-> defaults/site-defaults
                   (assoc-in
                    [:security :anti-forgery]
                    {:read-token (fn [req] (-> req :params :csrf-token))})
                   (assoc-in [:static :resources] "public"))]
    (-> handler
        (defaults/wrap-defaults config)
        (resource/wrap-resource "/META-INF/resources"))))

(defrecord Application []

  component/IComponent
  (-start [{:keys [websocket application resources options
                   files host routes scope port handlers] :as app}]
    (let [{:keys [ws ws-routes]} (if websocket
                                   (-> websocket
                                       (update-in [:handlers] merge (:websocket handlers))
                                       (create-websocket)))

          app-routes (if application
                       (-> application
                           (update-in [:handlers] merge (:application handlers))
                           (create-application)))
          all-routes (create-routes ws-routes app-routes resources files)
          server (server/server {:host host
                                 :port port
                                 :handler (wrap-app all-routes) 
                                 :options options})]
      (assoc app :websocket-server ws :http-server server)))
  
  (-stop [{:keys [websocket-server http-server] :as app}]
    (component/stop websocket-server)
    (component/stop http-server)
    (dissoc app :websocket-channel :http-server)))

(defn application [{:keys [websocket application host routes scope port handlers] :as m}]
  (-> (map->Application m)
      (component/start)))