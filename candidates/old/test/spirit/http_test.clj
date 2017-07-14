(ns spirit.transport-test
  (:require [spirit.transport :as http]))


(comment

  (require '[spirit.transport :as http])

  (def server (http/server {:type :http-kit
                            :host "test.spirit"
                            :port 7889
                            :websocket   {:path     "/ws"
                                          :spec     {:on/me :<spec/me>}}
                            :application {:path     "/api"
                                          :handlers {:on/me (fn [req] :on/me)}
                                          :routes   {:on/me "/me"}
                                          :spec     {:on/me :<spec/me>}}}))
  
  (def client (http/client {:type :http-kit
                            :host "test.spirit"
                            :port 7889
                            :application {:path     "/api"
                                          :routes   {:on/me "/me"}
                                          :spec     {:on/me :<spec/me>}}}))

  (def ws-client (http/ws-client {:type :jetty
                                  :host "test.spirit"
                                  :port 7889
                                  :websocket {:path     "/ws"
                                              :spec     {:on/me :<spec/me>}}}))
  )
