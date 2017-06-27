(ns spirit.http-test
  (:use hara.test)
  (:require [spirit.http :refer :all]
            [hara.component :as component]))


(comment
  
  (defmulti debug-handler :id)

  (defmulti application-handler :id)

  (defmulti event-handler :id)

  ((ring/->Files {:dir "resources/public"}))
  
  (def server  (application {:port 8900
                             :host "local.keynect.io"
                             :options   {:open-browser true}
                             :resources {:path "/"}
                             :files     {:path "/" :root "resources/root"}
                             :handlers  {:websocket   {:debug {}
                                                       :auth  {:event-handler default-event-handler
                                                               :request-handler default-request-handler}}
                                         :application {:debug {}
                                                       :auth  {:routes  [["login" :on/login]
                                                                         ["logout" :on/logout]]
                                                               :handler default-request-handler}}}
                             }))
  
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
