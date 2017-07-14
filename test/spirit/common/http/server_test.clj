(ns spirit.http.server-test
  (:use hara.test)
  (:require [spirit.http.server :refer :all]
            [spirit.http.transport :as transport]
            [hara.event :as event]))

^{:refer spirit.http.server/create :added "0.5"}
(fact "multimethod entrypoint for server construction")

^{:refer spirit.http.server/trim-seperators :added "0.5"}
(fact "gets rid of seperators on ends of string"

  (trim-seperators "//api/me//")
  => "api/me")

^{:refer spirit.http.server/wrap-trim-uri :added "0.5"}
(fact "gets rid of hanging seperator in :uri field"
  
  ((wrap-trim-uri identity)
   {:uri "/api/me/"})
  => {:uri "api/me"})

^{:refer spirit.http.server/wrap-path-uri :added "0.5"}
(fact "shortcuts the lookup in the uri path"
  
  ((wrap-path-uri identity "api")
   {:uri "api/me"})
  => {:uri "api/me"}

  ((wrap-path-uri identity "api")
   {:uri "other/me"})
  => nil)

^{:refer spirit.http.server/wrap-routes :added "0.5"}
(fact "gets rid of hanging seperator in :uri field"
  
  ((-> identity
       (wrap-routes {:on/me "api/me"}))
   {:uri "api/me"})
  => {:uri "api/me", :id :on/me}

  ((-> identity
       (wrap-routes {:on/me "/me"} {:path "/api/"}))
   {:uri "api/me"})
  => {:uri "api/me", :id :on/me}

  ((-> identity
       (wrap-routes {:on/me "/me"}
                    {:options {:navigation true}
                     :path "/api/"}))
   {:uri "api/lost"})
  => {:type :not-found, :status :redirect, :data {:on/me "api/me"}})

^{:refer spirit.http.server/wrap-parse-data :added "0.5"}
(fact "reads data from `:body` and store in `:data`"

  ((wrap-parse-data identity)
   {:body "{:a 1}"})
  => {:body "{:a 1}", :data {:a 1}})

^{:refer spirit.http.server/wrap-response :added "0.5"}
(fact "reads data from `:body` and store in `:data`"

  ((wrap-response identity)
   {:on/hello true})
  => {:type :reply
      :status :success
      :data {:on/hello true}}
  
  ((wrap-response (fn [_] (event/raise :on/hello)))
   nil)
  => {:type :reply
      :status :error
      :data {:on/hello true}}

  ((wrap-response (fn [_] (throw (ex-info "hello" {:on/hello true}))))
   nil)
  => {:type :reply,
      :status :error,
      :message "hello",
      :data {:on/hello true}})

^{:refer spirit.http.server/wrap-transport :added "0.5"}
(fact "packages the response for delivery via http"

  ((wrap-transport (fn [_] (response {:id :on/hello
                                      :data {:hello :world}})))
   nil)
  => {:headers {"Content-Type" "application/edn"},
      :status 200,
      :body "{:id :on/hello, :data {:hello :world}}"})

^{:refer spirit.http.server/base-handler :added "0.5"}
(fact "base handler for `:id` based dispatching"

  ((base-handler {:on/hello identity})
   {:id :on/hello
    :data {:hello :world}})
  => {:hello :world})

^{:refer spirit.http.server/wrappers :added "0.5"}
(comment "accesses default wrappers in the `common.server` namespace")

^{:refer spirit.http.server/wrap-handler :added "0.5"}
(fact "testing `transport/wrap-handler` for `base-handler`"

  (def app-handler
    (-> (base-handler {:on/me    (fn [data] {:on/me true})
                       :on/hello (fn [_] (event/raise :on/hello))})
        (transport/wrap-handler 
         [:wrap-parse-data
          :wrap-response
          :wrap-routes
          :wrap-path-uri
          :wrap-trim-uri
          :wrap-transport]
         {:format :edn
          :path "api"
          :routes {:on/me "me"
                   :on/hello "hello"}
          :options {:navigation true}}
         *default-wrappers*)))
  
  (app-handler {:uri "/api/me"})
  => {:headers {"Content-Type" "application/edn"},
      :status 200,
      :body "{:data {:on/me true}, :type :reply, :status :success, :id :on/me}"}

  (app-handler {:uri "/api/hello"})
  => {:headers {"Content-Type" "application/edn"},
      :status 200,
      :body "{:type :reply, :status :error, :data {:on/hello true}, :id :on/hello}"}

  (app-handler {:uri "/api/other"})
  => {:headers {"Content-Type" "application/edn"},
      :status 200,
      :body "{:type :not-found, :status :redirect, :data {:on/me \"api/me\", :on/hello \"api/hello\"}}"}
  
  (app-handler {:uri "/other"})
  => nil)


^{:refer spirit.http.server/create-application-handler :added "0.5"}
(fact "creates a handler based on config"

  (def app-handler (create-application-handler
                    *default-config*
                    {:path      "v1"
                     :routes    {:on/me    "me"}
                     :handlers  {:on/me    (fn [data] {:on/me true})}}))
  
  (app-handler {:uri "v1/me"})
  => {:headers {"Content-Type" "application/edn"},
      :status 200,
      :body "{:data {:on/me true}, :type :reply, :status :success, :id :on/me}"}

  (app-handler {:uri "v1/not-found"})
  => {:headers {"Content-Type" "application/edn"},
      :status 200,
      :body "{:type :not-found, :status :redirect, :data {:on/me \"v1/me\"}}"}

  (app-handler {:uri "lost"})
  => nil)

^{:refer spirit.http.server/create-handler :added "0.5"}
(fact "creates a single handler for application"

  (def main-handler
    (create-handler (assoc *default-config* :enable [:default :newest])
                    {:default {:path      "v1"
                               :format    :json
                               :routes    {:on/me    "me"
                                           :on/hello "hello"}
                               :handlers  {:on/me    (fn [data] {:on/me true})
                                           :on/hello (fn [_] (event/raise :on/hello))}}
                     :newest {:path       "v2"
                              ;;:format     :json
                              :routes     {:on/me    "me"
                                           :on/hello "hello"}
                              :handlers   {:on/me    (fn [data] {:v2/me true})
                                           :on/hello (fn [_] (event/raise :v2/hello))}}}))
  
  (-> (main-handler {:uri "v1"})
      :body
      (transport/read-value :json))
  => {:type "not-found",
      :status "redirect",
      :data {:on/me "v1/me", :on/hello "v1/hello"}}

  (-> (main-handler {:uri "v2"})
      :body
      (transport/read-value :edn))
  => {:type :not-found,
      :status :redirect,
      :data {:on/me    "v2/me",
             :on/hello "v2/hello"}})

(comment ""
  
  (def serv (httpkit/server {:host     "localhost"
                             :port     8000
                             :enable  [:default]
                             :applications
                             {:default {:path      "v1"
                                        :format    :json
                                        :routes    {:on/me    "me"
                                                    :on/hello "hello"}
                                        :handlers  {:on/me    (fn [data] {:on/me true})
                                                    :on/hello (fn [_] (event/raise :on/hello))}}
                              :newest {:path       "v2"
                                       :format     :json
                                       :routes     {:on/me    "me"
                                                    :on/hello "hello"}
                                       :handlers   {:on/me    (fn [data] {:on/me true})
                                                    :on/hello (fn [_] (event/raise :on/hello))}}}}))
  
  
  ;; to

  {:host     "localhost"
   :port     8000
   :enable  [:default]
   :applications
   {:default (-> (base-handler {:on/me    (fn [data] {:on/me true})
                                :on/hello (fn [_] (event/raise :on/hello))})
                 (transport/wrap-handler
                  [:wrap-parse-data
                   :wrap-response
                   :wrap-routes
                   :wrap-path-uri
                   :wrap-trim-uri
                   :wrap-transport]
                  {:path     "v1"
                   :format   :json
                   :routes   {:on/me    "me"
                              :on/hello "hello"}}
                  *default-wrappers*))}}
  
  
  (def serv (httpkit/server {:host     "localhost"
                             :port     8000
                             :format   :edn
                             :routes   {:on/me    "api/me"
                                        :on/hello "api/hello"}
                             :handlers {:on/me    (fn [data] {:on/me true})
                                        :on/hello (fn [_] (event/raise :on/hello))}}))
  
  (def serv (httpkit/server {:host     "localhost"
                             :port     8000
                             :format   :edn
                             :lookup   *default-wrappers*
                             :wrappers [:wrap-parse-data
                                        :wrap-response
                                        :wrap-routes
                                        :wrap-trim-uri
                                        :wrap-transport]
                             :routes   {:on/me    "api/me"
                                        :on/hello "api/hello"}
                             :handlers {:on/me    (fn [data] {:on/me true})
                                        :on/hello (fn [_] (event/raise :on/hello))}}))
  
  
  )
