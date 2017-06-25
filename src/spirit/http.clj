(ns spirit.http
  (:require [spirit.http
             [handler :as handler]
             [route :as route]
             [server :as server]
             [websocket :as websocket]]
            [hara.component :as component]
            [bidi.bidi :as bidi]))

(defmulti debug-handler :id)

(defmulti application-handler :id)

(defmulti event-handler :id)


(comment
  
  (def api-routes
    ["/" {""           :index
          "index.html" :index
          "articles/" {"index.html" :article-index
                       [:id "/article.html"] :article}}])
  
  
  (http-server {:port 8900
                :host "local.keynect.io"
                :resources {:path "/"}
                :websocket {:path       "/ws"
                            :packer     :edn
                            :scope      [:debug :application]
                            :allowed    [:on/login]
                            :handlers   {:debug {:event-handler  debug-event-handler
                                                :request-handler debug-request-handler
                                                :wrappers []}
                                        :app   {:event-handler event-handler
                                                :request-handler application-handler
                                                :wrappers []}}}
                :api       {:packer     :edn
                            :scope      [:debug :application]
                            :allowed    [:on/login]
                            :handlers   {:debug {:routes  debug-routes
                                                 :handler debug-handler}
                                         :application {:routes application-routes
                                                       :handler application-handler}}}})
  
  (bidi/match-route my-routes "/eueu")
  (bidi/match-route my-routes "/articles/oeuo/article.html")
  
  (defn wrap-id [ my-routes])
  
  (def server (server/server {:port 8090
                              :handler (fn [_] {:body "hello world"})
                              :options {:open-browser true}}))
  
  (component/stop server)
  
  (defmethod msg-handler :on/login
    [m]
    (println))

  )
