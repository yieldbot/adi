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

^{:refer spirit.http/create-websocket-routes :added "0.5"}
(fact "specified using the 'path' input")

^{:refer spirit.http/create-websocket-handler :added "0.5"}
(fact "takes in a table of handlers and also the scope"
  
  (create-websocket-handler
   {:debug {:event-handler   :<debug-event>>
            :request-handler :<debug-request>}
    :auth  {:event-handler   :<auth-event>
            :request-handler :<auth-request>}}
   [:auth :debug]))

^{:refer spirit.http/create-websocket :added "0.5"}
(fact "creates a websocket given a config"
  
  (create-websocket
   {:path       "/ws"
    :packer     :edn
    :scope      [:auth :debug]
    :handlers   {:debug {}
                 :auth  {:event-handler   :<auth-event>
                         :request-handler :<auth-request>}}}))

^{:refer spirit.http/create-application-handler :added "0.5"}
(fact "creates a set of routes for http consumption"
 
  (create-application-handler
   "/api"
   {:debug {:routes   :<debug-routes>>
            :handler  :<debug>}
    :auth  {:routes   :<auth-routes>
            :handler  :<auth>}}
   [:auth :debug]))

^{:refer spirit.http/create-application :added "0.5"}
(fact "creates the set of application routes for consumption"
  
  (create-application
   {:scope     [:auth :debug]
    :handlers  {:debug {}
                :auth  {:routes  :<auth-routes>
                        :handler :<auth>}}}))

^{:refer spirit.http/create-routes :added "0.5"}
(fact "adds both websocket and application routes")

^{:refer spirit.http/wrap-app :added "0.5"}
(fact "adds defaults to app")

^{:refer spirit.http/application :added "0.5"}
(comment
  
  (def sys (application {:port 8900
                         :host "local.keynect.io"
                         :options   {:open-browser true}
                         :resources {:path "/"}
                         :files     {:path "/" :root "resources/public"}
                         :handlers  {:websocket   {:debug {}
                                                   :auth  {:event-handler :<auth-event>
                                                           :request-handler :<auth-request>}}
                                     :application {:debug {}
                                                   :auth  {:routes  [["login" :on/login]
                                                                     ["logout" :on/logout]]
                                                           :handler :<auth-request>}}}})))


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
