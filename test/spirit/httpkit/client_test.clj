(ns spirit.httpkit.client-test
  (:use hara.test)
  (:require [spirit.httpkit.client :refer :all]
            [spirit.common.http.client :as client]
            [spirit.common.http.server :as server]
            [hara.component :as component]))

^{:refer spirit.httpkit.client/query-string :added "0.5"}
(fact "create a query string from a map"

  (query-string {})
  => ""
  
  (query-string {:timing true})
  => "?timing=true")

^{:refer spirit.httpkit.client/create-url :added "0.5"}
(fact "`create-url` for client"

  (def cl
    (client/create {:type      :httpkit
                    :protocol  "http"
                    :host      "localhost"
                    :port      8001
                    :path      "api"
                    :routes    {:on/id "id"}}))
  
  (create-url cl :on/id {})
  => "http://localhost:8001/api/id")

^{:refer spirit.httpkit.client/http-post :added "0.5"}
(fact "httpkit function to client"

  (def cl
    (client/create {:type      :httpkit
                    :protocol  "http"
                    :host      "localhost"
                    :port      8001
                    :path      "api"
                    :routes    {:on/id "id"}}))
  
  (-> @(http-post cl {:id :on/id
                      :params {:timing true}
                      :data {}}
                  {})
      :opts
      :url)
  => "http://localhost:8001/api/id?timing=true")

^{:refer spirit.httpkit.client/wrap-response-errors :added "0.5"}
(fact "wrap errors ")



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

