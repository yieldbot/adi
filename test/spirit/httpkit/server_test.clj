(ns spirit.httpkit.server-test
  (:use hara.test)
  (:require [spirit.http.server :as common]
            [spirit.http.transport :as transport]
            [spirit.httpkit.server :refer :all]
            [org.httpkit.client :as client]
            [hara.component :as component]
            [hara.event :as event]))

^{:refer spirit.httpkit.server/server :added "0.5"}
(fact "creating httpkit server"

  (def sys (server {:handler (fn [req] (prn req) {:status 200 :body "hello world"})}))
  
  (-> @(client/get "http://localhost:8000" {:as :text})
      :body)
  => "hello world"

  (component/stop sys))

^{:refer spirit.http.server/create :added "0.5"}
(fact "creating httpkit server"
  
  (def sys (-> (common/create {:type     :httpkit
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
  
  (-> @(client/get "http://localhost:8001/debug/me" {:as :text})
      :body
      (transport/read-value :json))
  => {:data {:on/me true}, :type "reply", :status "success", :id "on/me"}

  (component/stop sys))
