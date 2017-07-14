(ns spirit.http.server
  (:require [spirit.http.transport :as transport]
            [hara.event :as event]
            [cheshire.core :as json]))

(defmulti create
  "multimethod entrypoint for server construction"
  {:added "0.5"}
  :type)

(defmethod transport/read-body java.io.InputStream
  [body format]
  (transport/read-value (slurp body) format))

(defmethod transport/read-value :json
  [body format]
  (json/parse-string body keyword))

(defmethod transport/write-value :json
  [body _]
  (json/generate-string body))

(defn trim-seperators
  "gets rid of seperators on ends of string
 
   (trim-seperators \"//api/me//\")
   => \"api/me\""
  {:added "0.5"}
  [s]
  (let [ns (cond-> s
             (.startsWith s "/")
             (-> (subs 1))
             
             (.endsWith s "/")
             (-> (subs 0 (dec (count s)))))]
    (if (= (count ns) (count s))
      ns
      (trim-seperators ns))))

(defn wrap-trim-uri
  "gets rid of hanging seperator in :uri field
   
   ((wrap-trim-uri identity)
    {:uri \"/api/me/\"})
   => {:uri \"api/me\"}"
  {:added "0.5"}
  [handler]
  (fn [req]
    (handler (update-in req [:uri] trim-seperators))))

(defn wrap-path-uri
  "shortcuts the lookup in the uri path
   
   ((wrap-path-uri identity \"api\")
    {:uri \"api/me\"})
   => {:uri \"api/me\"}
 
   ((wrap-path-uri identity \"api\")
    {:uri \"other/me\"})
   => nil"
  {:added "0.5"}
  ([handler]
   (wrap-path-uri handler nil))
  ([handler path]
   (fn [{:keys [uri] :as req}]
     (if (.startsWith uri (trim-seperators path))
       (handler req)))))

(defn wrap-routes
  "gets rid of hanging seperator in :uri field
   
   ((-> identity
        (wrap-routes {:on/me \"api/me\"}))
    {:uri \"api/me\"})
   => {:uri \"api/me\", :id :on/me}
 
   ((-> identity
        (wrap-routes {:on/me \"/me\"} {:path \"/api/\"}))
    {:uri \"api/me\"})
   => {:uri \"api/me\", :id :on/me}
 
   ((-> identity
        (wrap-routes {:on/me \"/me\"}
                     {:options {:navigation true}
                      :path \"/api/\"}))
    {:uri \"api/lost\"})
   => {:type :not-found, :status :redirect, :data {:on/me \"api/me\"}}"
  {:added "0.5"}
  ([handler routes]
   (wrap-routes handler routes {}))
  ([handler routes {:keys [path options]}]
   (let [uris   (->> (vals routes)
                     (map trim-seperators)
                     (map (fn [uri]
                            (if path
                              (str (trim-seperators path) "/" uri)
                              uri))))
         ids        (keys routes)
         routes     (zipmap ids uris)
         rev-routes (zipmap uris (keys routes))]
     (fn [{:keys [uri] :as req}]
       (if-let [id (get rev-routes uri)]
         (let [result (handler (assoc req :id id))]
           (assoc result :id id))
         (cond (:navigation options)
               (transport/response {:type :not-found
                                    :status :redirect
                                    :data routes})

               :else
               (transport/response {:type :not-found
                                    :data {:uri uri}})))))))

(defn wrap-parse-data
  "reads data from `:body` and store in `:data`
 
   ((wrap-parse-data identity)
    {:body \"{:a 1}\"})
   => {:body \"{:a 1}\", :data {:a 1}}"
  {:added "0.5"}
  ([handler]
   (wrap-parse-data handler :edn))
  ([handler format]
   (fn [{:keys [body] :as req}]
     (let [data (transport/read-body body format)]
       (handler (assoc-in req [:data] data))))))

(defn wrap-response
  "reads data from `:body` and store in `:data`
 
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
 
   ((wrap-response (fn [_] (throw (ex-info \"hello\" {:on/hello true}))))
    nil)
   => {:type :reply,
       :status :error,
       :message \"hello\",
       :data {:on/hello true}}"
  {:added "0.5"}
  [handler]
  (fn [req]
    (event/manage 
     (let [result  (handler req)
           result  (if (transport/response? result)
                     result
                     (transport/response {:data (or result {})}))]
       (assoc result :type :reply :status :success))
     (on _ issue
         (transport/response {:type    :reply
                              :status  :error
                              :data    issue}))
     (catch clojure.lang.ExceptionInfo e
       (transport/response {:type    :reply
                            :status  :error
                            :message (.getMessage e)
                            :data    (ex-data e)})))))

(defn wrap-transport
  "packages the response for delivery via http
 
   ((wrap-transport (fn [_] (response {:id :on/hello
                                       :data {:hello :world}})))
    nil)
   => {:headers {\"Content-Type\" \"application/edn\"},
       :status 200,
       :body \"{:id :on/hello, :data {:hello :world}}\"}"
  {:added "0.5"}
  ([handler]
   (wrap-transport handler :edn))
  ([handler format]
   (fn [req]
     (let [result (handler req)]
       (cond (nil? result)
             nil
             
             (transport/response? result)
             {:headers {"Content-Type" (case format
                                         :edn "application/edn"
                                         :json "application/json")}
              :status 200
              :body (transport/write-value (into {} result) format)}

             :else
             (throw (ex-info "TRANSPORT SHOULD CONTAIN A RESPONSE" {})))))))

(defn wrap-timing
  [handler]
  (fn [{:keys [params] :as req}]
    (cond (:timing params)
          (let [start    (System/nanoTime)
                response (handler req)
                end      (System/nanoTime)]
            (assoc-in response [:meta :timing] (- end start)))

          :else
          (handler req))))

(defn wrap-delay
  [handler]
  (fn [{:keys [params] :as req}]
    (if-let [delay (:delay params)]
      (do (Thread/sleep delay)
          (handler req))
      (handler req))))

(defn wrap-all [handlers wrapper]
  (reduce-kv (fn [out k v]
               (assoc out k (wrapper v)))
             {}
             handlers))

(defn base-handler
  "base handler for `:id` based dispatching
 
   ((base-handler {:on/hello identity})
    {:id :on/hello
     :data {:hello :world}})
   => {:hello :world}"
  {:added "0.5"}
  ([handlers]
   (base-handler handlers {}))
  ([handlers options]
   (let [handlers (cond-> (wrap-all handlers wrap-response)
                    
                    (:timing options)
                    (wrap-all wrap-timing)
                    
                    (:delay options)
                    (wrap-all wrap-delay))]
     (fn [{:keys [id data] :as req}]
       (if-let [handler (get handlers id)]
         (handler data))))))

(def ^:dynamic *default-wrapper-lookup*
  {:wrap-transport      {:func wrap-transport
                         :args [[:format]]}
   :wrap-trim-uri       {:func wrap-trim-uri}
   :wrap-path-uri       {:func wrap-path-uri
                         :args [[:path]]}
   :wrap-routes         {:func wrap-routes
                         :args [[:routes] []]}
   :wrap-parse-data     {:func wrap-parse-data
                         :args [[:format]]}})

(def ^:dynamic *default-wrapper-list*
  [:wrap-parse-data
   :wrap-routes
   :wrap-path-uri
   :wrap-trim-uri
   :wrap-transport])

(def ^:dynamic *default-config*
  (merge transport/*default-config*
         {:enable   [:default]
          :wrapper  {:list   *default-wrapper-list*
                     :lookup *default-wrapper-lookup*}
          :options  {:navigation true}}))

(defn create-application-handler
  "creates a handler based on config
 
   (def app-handler (create-application-handler
                     *default-config*
                     {:path      \"v1\"
                      :routes    {:on/me    \"me\"}
                      :handlers  {:on/me    (fn [data] {:on/me true})}}))
   
   (app-handler {:uri \"v1/me\"})
   => {:headers {\"Content-Type\" \"application/edn\"},
       :status 200,
       :body \"{:data {:on/me true}, :type :reply, :status :success, :id :on/me}\"}
 
   (app-handler {:uri \"v1/not-found\"})
   => {:headers {\"Content-Type\" \"application/edn\"},
       :status 200,
       :body \"{:type :not-found, :status :redirect, :data {:on/me \\\"v1/me\\\"}}\"}
 
   (app-handler {:uri \"lost\"})
   => nil"
  {:added "0.5"}
  [{:keys [wrapper] :as config}
   {:keys [routes handlers path] :as application}]
  (-> (base-handler handlers)
      (transport/wrap-handler (:list wrapper)
                              (merge config application)
                              (:lookup wrapper))))

(defn create-handler
  "creates a single handler for application
 
   (def main-handler
     (create-handler (assoc *default-config* :enable [:default :newest])
                     {:default {:path      \"v1\"
                                :format    :json
                                :routes    {:on/me    \"me\"
                                            :on/hello \"hello\"}
                                :handlers  {:on/me    (fn [data] {:on/me true})
                                            :on/hello (fn [_] (event/raise :on/hello))}}
                      :newest {:path       \"v2\"
                               ;;:format     :json
                               :routes     {:on/me    \"me\"
                                            :on/hello \"hello\"}
                               :handlers   {:on/me    (fn [data] {:v2/me true})
                                            :on/hello (fn [_] (event/raise :v2/hello))}}}))
   
   (-> (main-handler {:uri \"v1\"})
       :body
       (transport/read-value :json))
   => {:type \"not-found\",
      :status \"redirect\",
       :data {:on/me \"v1/me\", :on/hello \"v1/hello\"}}
 
   (-> (main-handler {:uri \"v2\"})
       :body
       (transport/read-value :edn))
   => {:type :not-found,
       :status :redirect,
       :data {:on/me    \"v2/me\",
              :on/hello \"v2/hello\"}}"
  {:added "0.5"}
  [{:keys [enable] :as config} applications]
  (let [applications (if enable
                       (map applications enable)
                       (vals applications))
        handlers (map #(create-application-handler config %) applications)]
    (fn [req] (some (fn [handler] (handler req)) handlers))))
