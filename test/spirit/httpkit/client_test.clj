(ns spirit.httpkit.client-test
  (:use hara.test)
  (:require [spirit.httpkit
             [client :refer :all]
             server]
            [spirit.http.client :as client]
            [spirit.http.server :as server]
            [hara.component :as component]
            [hara.event :as event]
            [clojure.core.async :as async]))

(defn new-httpkit-server []
  (-> (server/create {:type     :httpkit
                      :protocol "http"
                      :host     "localhost"
                      :port     8001
                      :format   :edn
                      :enable   [:v1]
                      :applications
                      {:v1   {:path     "v1"
                              :routes   {:on/id "id"
                                         :on/error "error"}
                              :handlers {:on/id (fn [data] {:on/id true})
                                         :on/error (fn [data] (event/raise :on/error))}}}})
      (component/start)))

(defn new-httpkit-client []
  (client/create {:type      :httpkit
                  :protocol  "http"
                  :host      "localhost"
                  :port      8001
                  :path      "v1"
                  :routes    {:on/id "id"
                              :on/error "error"}}))

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
  (def server (new-httpkit-server))
  
  (def client (new-httpkit-client))

  (-> (http-post client {:id :on/id})
      deref
      :body
      read-string)
  => {:id :on/id
      :type :reply
      :status :success
      :data {:on/id true}}
  
  (-> (http-post client {:id :on/error})
      deref
      :body
      read-string)
  => {:id :on/error
      :type :reply
      :status :error
      :data {:on/error true}}

  (component/stop server))

^{:refer spirit.httpkit.client/process-response :added "0.5"}
(fact "processes the response - either errors or success"
  
  (def server (new-httpkit-server))

  (def client (new-httpkit-client))
  
  (process-response
   @(http-post client {:id :on/id})
   client
   {:id :on/id})
  => {:data {:on/id true}, :type :reply, :status :success, :id :on/id}

  (component/stop server)
  
  (process-response
   @(http-post client {:id :on/id})
   client
   {:id :on/id})
  => (contains-in {:opts {:as :text,
                          :body "nil",
                          :method :post,
                          :url "http://localhost:8001/v1/id"},
                   :id :on/id,
                   :type :reply,
                   :status :error,
                   :data {:exception java.net.ConnectException},
                   :input nil}))

^{:refer spirit.httpkit.client/return-channel :added "0.5"}
(fact "the return channel process compatible with core.async"
  
  (def server (new-httpkit-server))

  (def client (new-httpkit-client))
  
  (async/<!! (return-channel http-post client {:id :on/id}))
  => {:data {:on/id true}, :type :reply, :status :success, :id :on/id}

  (component/stop server)
  (async/<!! (return-channel http-post client {:id :on/id}))
  => (contains-in
      {:opts {:as :text, :body "nil", :method :post, :url "http://localhost:8001/v1/id"},
       :id :on/id,
       :type :reply,
       :status :error,
       :data {:exception java.net.ConnectException},
       :input nil}))

^{:refer spirit.httpkit.client/return-promise :added "0.5"}
(fact "the return channel process as a promise"

  (def server (new-httpkit-server))
  
  (def client (new-httpkit-client))
  
  (deref (return-promise http-post client {:id :on/id}))
  => {:id :on/id
      :type :reply
      :status :success 
      :data {:on/id true}}
  
  (component/stop server)

  (deref (return-promise http-post client {:id :on/id}))
  => (contains-in
      {:opts {:as :text, :body "nil", :method :post, :url "http://localhost:8001/v1/id"},
       :id :on/id,
       :type :reply,
       :status :error,
       :data {:exception java.net.ConnectException},
       :input nil}))

^{:refer spirit.httpkit.client/wrap-return :added "0.5"}
(fact "returns the required interface depending on `:return` value"

  (def server (new-httpkit-server))
  
  (def client (new-httpkit-client))
  
  ((wrap-return http-post)
   (assoc client :return :value)
   {:id :on/id})
  => {:id :on/id
      :type :reply
      :status :success
      :data {:on/id true}}

  (-> ((wrap-return http-post)
       (assoc client :return :channel)
       {:id :on/id})
      (async/<!!))
  => {:id :on/id
      :type :reply
      :status :success
      :data {:on/id true}}

  (-> ((wrap-return http-post)
       (assoc client :return :promise)
       {:id :on/id})
      (deref))
  => {:id :on/id
      :type :reply
      :status :success
      :data {:on/id true}}

  (component/stop server))

^{:refer spirit.httpkit.client/httpkit-client :added "0.5"}
(fact "creates a httpkit client for http transport"

  (def server (new-httpkit-server))
  
  (-> (httpkit-client {:port     8001
                       :return   :value ;; :promise :data
                       :path     "v1"
                       :routes   {:on/id "id"
                                  :on/error "error"}})
      (client/request {:id :on/id}))
  => {:id :on/id
      :type :reply
      :status :success
      :data {:on/id true}}
  
  (component/stop server))

(comment
  
  (def sys (-> (server/create {:type     :httpkit
                               :protocol "http"
                               :host     "localhost"
                               :port     8001
                               :format   :edn
                               :enable   [:debug]
                               :applications
                               {:debug   {:path     "debug"
                                          :routes   {:on/me    "me"
                                                     :on/hello "hello"}
                                          :handlers {:on/me    (fn [data] {:on/me true})
                                                     :on/hello (fn [_] (event/raise :on/hello))}}}})
               (component/start)))

  
  
  (component/stop sys)
  
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

