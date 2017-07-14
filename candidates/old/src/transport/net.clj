(ns spirit.transport.net)

(def ^:dynamic *default-config*
  {:protocol "http"
   :host     "localhost"
   :port     8000
   :format   :edn})

(defmulti write-value
  "writes the string value of the datastructure according to format
 
   (write-value {:a 1} :edn)
   => \"{:a 1}\""
  {:added "0.5"}
  (fn [s format] format))

(defmethod write-value :edn
  [s _]
  (pr-str s))

(defmulti read-value
  "read the string value of the datastructure according to format
 
   (read-value \"{:a 1}\" :edn)
   => {:a 1}"
  {:added "0.5"}
  (fn [s format] format))

(defmethod read-value :edn
  [s _]
  (read-string s))

(defprotocol IConnection
  (-send    [conn {:keys [id header body]}])
  (-request [conn {:keys [id header body]}]))

(defn )

(defmulti read-body
  "reads the body of the request can be expanded
 
   (read-body \"{:a 1}\" :edn)
   => {:a 1}"
  {:added "0.5"}
  (fn [body format] (type body)))

(defmethod read-body nil
  [body _]
  {})

(defmethod read-body String
  [body format]
  (read-value body format))

(defn wrap-handler
  "wraps a handler given a lookup and a config
 
   ((wrap-handler identity
                   [:add-number
                    :mul-number]
                   {:ops {:add 5 :mul 10}}         
                   {:add-number {:func (fn [handler arg]
                                         (fn [number]
                                          (+ (handler number) arg)))
                                 :args [[:ops :add]]}
                    :mul-number {:func (fn [handler arg]
                                         (fn [number]
                                           (* (handler number) arg)))
                                 :args [[:ops :mul]]}})
    10)
   => 150"
  {:added "0.5"}
  [handler wrappers config lookup]
  (reduce (fn [out k]
            (let [{:keys [func args]} (get lookup k)]
              (if-not func (throw (ex-info (str "Missing function for key") {:key k})))
              (apply func out (map #(get-in config %) args))))
          handler
          wrappers))

(defrecord Response []
  Object
  (toString [res]
    (str "#response" (into {} (if (get-in res [:data :exception])
                                (update-in res [:data :exception] type)
                                res)))))

(defmethod print-method Response
  [v w]
  (.write w (str v)))

(defn response
  "constructs a Response object
 
   (response {:id :on/info
              :header {:token \"123password\"}
              :data {:name \"Chris\"}})
   => spirit.transport.server.Response"
  {:added "0.5"}
  [{:keys [id data] :as m}]
  (map->Response m))

(defn response?
  "checks if data is a response"
  {:added "0.5"}
  [response]
  (instance? Response response))
