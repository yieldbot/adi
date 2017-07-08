(ns spirit.httpkit.client-test
  (:use hara.test)
  (:require [spirit.httpkit.client :refer :all]
            [spirit.common.http.client :as client]
            [spirit.common.http.server :as server]))

(comment
  
  (def sys (-> (server/create {:type     :httpkit
                               :protocol "http"
                               :host     "localhost"
                               :port     8001
                               :format   :json
                               :enable   [:debug]
                               :applications
                               {:debug   {:path     "debug"
                                          :routes   {:on/me    "me"
                                                     :on/hello "hello"}
                                          :handlers {:on/me    (fn [data] {:on/me true})
                                                     :on/hello (fn [_] (event/raise :on/hello))}}}})
               (component/start)))
  
  (def client (client/create {:type :httpkit
                              :protocol "http"
                              :host     "localhost"
                              :port     8001
                              :format   :json
                              :return   :channel ;; :promise :data
                              :path     "debug"
                              :routes   {:on/me    "me"
                                         :on/hello "hello"}}))
  
  (client/request client {:id :on/me})
  => {:id   :on/me
      :type :reply
      :data {:on/me true}}
  

  )

