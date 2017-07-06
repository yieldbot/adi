(ns spirit.http.transport)


(defn application [m])

(defn http-client [m])
(defn ws-client [m])


(defmulti -push    (fn [http {:keys [id header body]} opts]
                    (:type http)))

(defmulti -request (fn [http {:keys [id header body]} opts]
                    (:type http)))

(defn push
  ())

(comment
  ;; simplest example of client/server architecture

  
  (def server (application {:host "test.spirit"
                            :port 7889
                            :application {:path     "/api"
                                          :scope    [:on/auth :on/debug]
                                          :function {:on/debug {:handlers {:on/system.info (fn [req] :on/system.info)}
                                                                :routes   {:on/system.info "/system/info"}
                                                                :spec     {}}
                                                     :on/auth  {:handlers {:on/me (fn [req] :on/me)}
                                                                :routes   {:on/me "/me"}
                                                                :spec     {}}}}}))
  
  (def client (http-client {:type :http-kit
                            :host "test.spirit"
                            :port 7889
                            :application {:path     "/api"
                                          :scope    [:on/auth :on/debug]
                                          :function {:on/debug {:routes   {:on/system.info "/system/info"}
                                                                :spec     {}}
                                                     :on/auth  {:routes   {:on/me "/me"}
                                                                :spec     {}}}}}))

  (request client
           {:id     :on/account.me
            :type   :request
            :header {}
            :data   {:on/token "c57d895b-777f-42f0-b331-d22d337cc0a4"}})
  

  
  (def ws (ws-client {:host "test.spirit"
                      :port 7889
                      :websocket {:path "/ws"
                                  :scope  [:auth :debug]
                                  :function {:on/debug {:handlers {:on/ping (fn [req] :on/pong)}}
                                             :on/auth  {:handlers {:on/user.client (fn [req] :client.information)}}}}}))
  
  
  )


(comment
  ;; transport takes care of constructing the communication between client and server
  ;; the client constructs both the request and the response. The default response will be
  ;; type :reply
  
  ;; for responses that are :push or :broadcast which are only there for ws-clients we will
  ;; have handlers on the clients themselves

  
  (defn http-client [])
  (defn ws-client [])

  (ws-client {:handlers
              {:on/account.info (fn [{:keys [id meta data]}]
                                  (println data))}})
  
  
  (let [response {}])
  
  (request client
           {:id     :on/account.me
            :header {}
            :data   {:on/token "c57d895b-777f-42f0-b331-d22d337cc0a4"}})
  
  (request client
           {:id     :on/account.me
            :header {}
            :data   {:on/token "c57d895b-777f-42f0-b331-d22d337cc0a4"}})
  => #{:id
       :status    ;;    :response, :exception, :redirect, :broadcast, :push
       :header
       :data}

  :response
  
  {:id     :on/account.me
   :type   :response
   :meta   {:content-type "text/plain"}
   :data   {:username "zcaudate"
            :links {}}}
  
  {:id     :on/account.accounts
   :type   :response
   :header {:content-type "text/plain"
            :navigation {:prev {:page "1"} :next {:page "3"}}}
   :data   {:username "zcaudate"}}
  
  :exception

  {:id     :on/account.me
   :type   :exception
   :header {:request {}}
   :data   {}}
  
  :redirect ;; (for deprecated
  
  {:id     :on/account.me
   :type   :redirect
   :header {}
   :data   {}}
  
  )
