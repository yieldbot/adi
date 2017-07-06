(ns spirit.http-test
  (:use hara.test)
  (:require [spirit.http :refer :all]
            [hara.component :as component]
            [org.httpkit.client :as http]))

^{:refer spirit.http/default-event-handler :added "0.5"}
(fact "default handler for events from client")

^{:refer spirit.http/default-request-handler :added "0.5"}
(fact "default handler for requests from client")

^{:refer spirit.http/wrap-scope :added "0.5"}
(fact "adds a scope key to the request")

^{:refer spirit.http/adjust-path :added "0.5"}
(fact "adds separators to the input path"

  (adjust-path "api")
  => "/api/")

^{:refer spirit.http/create-websocket-routes :added "0.5"}
(fact "specified using the 'path' input")

^{:refer spirit.http/create-websocket-handler :added "0.5"}
(fact "takes in a table of handlers and also the scope"
  
  (def handler (create-websocket-handler
                {:debug {:request-handlers {:on/debug  (fn [req] :debug)}}
                 :auth  {:event-handlers   {:chsk/ws-ping identity}
                         :request-handlers {:on/login  (fn [req] :login)
                                            :on/logout (fn [req] :logout)}}}
                [:auth :debug]))
  
  (handler {:id :chsk/ws-ping})
  => {:id :chsk/ws-ping, :on/scope :auth}

  (handler {:id :on/login :?reply-fn identity})
  => {:id :on/login, :on/data :login :on/scope :auth})

^{:refer spirit.http/create-websocket :added "0.5"}
(fact "creates a websocket given a config"
  
  (create-websocket
   {:path       "ws"
    :packer     :edn
    :scope      [:auth :debug]
    :handlers   {:debug {:request-handlers {:on/debug  (fn [req] :debug)}}
                 :auth  {:event-handlers   {:chsk/ws-ping prn}
                         :request-handlers {:on/login  (fn [req] :login)
                                            :on/logout (fn [req] :logout)}}}})
  
  )

^{:refer spirit.http/create-application-handler :added "0.5"}
(fact "creates a set of routes for http consumption"
 
  (def handler (create-application-handler
                "api"
                {:on/login  (fn [req] :login)
                 :on/logout (fn [req] :logout)}
                {"login" :on/login, "logout" :on/logout}))
  
  (handler {:uri "/api/login"})
  => {:status 200, :headers {"Content-Type" "text/plain"}, :body ":login"})

^{:refer spirit.http/create-application :added "0.5"}
(fact "creates the set of application routes for consumption"
  
  (def handler (create-application
                {:path "api"
                 :scope     [:auth :debug]
                 :handlers  {:debug {:routes   {"debug"    :on/debug}
                                     :handlers {:on/debug  (fn [req] :debug)}}
                             :auth  {:routes   {"login"  :on/login,
                                                "logout" :on/logout}
                                     :handlers {:on/login  (fn [req] :login)
                                                :on/logout (fn [req] :logout)}}}}))
  (handler {:uri "/api/logout"})
  => {:status 200, :headers {"Content-Type" "text/plain"}, :body ":logout"}

  (handler {:uri "/api/debug"})
  => {:status 200, :headers {"Content-Type" "text/plain"}, :body ":debug"})

^{:refer spirit.http/create-routes :added "0.5"}
(fact "adds both websocket and application routes")

^{:refer spirit.http/wrap-app :added "0.5"}
(fact "adds defaults to app")

^{:refer spirit.http/application :added "0.5"}
(comment "creates an application server for api and we"
  
  (def sys (application {:port 8903
                         :host "local.keynect.io"
                         :application {:path   "api"
                                       :scope  [:auth]}
                         ;; :options   {:open-browser true}
                         :resources {:path "/"}
                         :files     {:path "/" :root "resources/public"}
                         :handlers  {:application {:debug {}
                                                   :auth  {:routes  {"login"  :on/login
                                                                     "logout" :on/logout}
                                                           :handlers {:on/login  (fn [req] :login)
                                                                      :on/logout (fn [req] :logout)}}}}}))
  
  
  (component/stop sys)
  )
  


(comment
  
  
  (defmulti debug-handler :id)

  (defmulti application-handler :id)

  (defmulti event-handler :id)

  ((ring/->Files {:dir "resources/public"}))
  
  (def server  )
  
  (component/stop server)
  
  
  (bidi/match-route my-routes "/eueu")
  (bidi/match-route my-routes "/articles/oeuo/article.html")
  
  (defn wrap-id [ my-routes])
  
  (def server (server/server {:port 8090
                              :handler (fn [_] {:body "hello world"})
                              }))
  
  (component/stop server)
  
  (defmethod msg-handler :on/login
    [m]
    (println))

  )


(comment
  
  (def server (server/server
               {:port 7564
                :handler (create-application
                          {:path "api"
                           :scope     [:auth :debug]
                           :handlers  {:debug {}
                                       :auth  {:routes  [["login" :on/login]
                                                         ["logout" :on/logout]]
                                               :handler default-request-handler}}})}))
  
  
  (component/stop server)
  (def server (server/server {:port 7564 :handler (wrap-api-handler "/api"
                                                                    default-request-handler
                                                                    [["login" :on/login]
                                                                     ["logout" :on/logout]])}))
  
  
  (create-application {:scope     [:auth :debug]
                       :handlers  {:debug {}
                                   :auth  {:routes  application-routes
                                           :handler application-handler}}})
  
  (create-websocket {:path       "/ws"
                     :scope      [:auth :debug]
                     :handlers   {:debug {}
                                  :auth  {:event-handler event-handler
                                          :request-handler application-handler}}})

  (def routes ["/" [["login"  :on/login]
                    ["logout" :on/logout]
                    [["account/" [#".*" :id]] :on/logout]]])
  
  (bidi/match-route routes "/account/12234")
)
