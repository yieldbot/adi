(ns spirit.httpkit-test
  (:require [spirit.common.http :as http]
            [spirit.httpkit.server :as server]
            [spirit.httpkit.client :as client]))

(comment
  
  (def server     (http/server {:type :httpkit
                                :host "test.spirit"
                                :port 7889
                                ;; :websocket   {:path     "/ws"
                                ;;               :spec     {:on/me :<spec/me>}}
                                :application {:path     "/api"
                                              :format   :edn
                                              :handlers {:on/me (fn [req] :on/me)}
                                              :routes   {:on/me "/me"}
                                              :spec     {:on/me :<spec/me>}}}))
  
  (def client     (http/client {:type :httpkit
                                :host "test.spirit"
                                :port 7889
                                :application {:path     "/api"
                                              :routes   {:on/me "/me"}
                                              :spec     {:on/me :<spec/me>}}})))

(comment

  (def server    (http/server    {:type :httpkit
                                  :host "test.spirit"
                                  :port 7889
                                  :websocket   {:path     "/ws"
                                                :spec     {:on/me :<spec/me>}}}))

  (def ws-client (http/ws-client {:type :jetty
                                  :host "test.spirit"
                                  :port 7889
                                  :websocket {:path     "/ws"
                                              :spec     {:on/me :<spec/me>}}})))

