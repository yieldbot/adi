(ns spirit.network.common
  (:require [clojure.core.async :as async]))

(defrecord Response []
  Object
  (toString [res]
    (str "#response" (into {} res))))

(defmethod print-method Response
  [v w]
  (.write w (str v)))

(defn response
  "creates a response from map
 
   (response {:type :on/id})
   ;;=> #response{:type :on/id}
   "
  {:added "0.5"}
  [m]
  (map->Response m))

(defn response?
  "checks if the object is a response
 
   (-> (response {:type :on/id})
       (response?))
   => true"
  {:added "0.5"}
  [x]
  (instance? Response x))

(defmulti read-value
  "reads a clojure data structure from string
 
   (read-value \"{:a 1 :b 2}\" :edn)
   => {:a 1 :b 2}"
  {:added "0.5"}
  (fn [string format] format))

(defmulti write-value
  "writes a clojure data structure to string
 
   (write-value [1 2 {:a 3}] :edn)
   => \"[1 2 {:a 3}]\""
  {:added "0.5"}
  (fn [data format] format))

(defmethod read-value :edn
  [string _]
  (read-string string))

(defmethod write-value :edn
  [data _]
  (pr-str data))

(defn pack
  "prepares the package for transport
 
   (pack {:format :edn} {:type :on/id})
   => \"{:type :on/id}\""
  {:added "0.5"}
  [{:keys [format]} package]
  (try
    (write-value package format)
    (catch Exception e
      (write-value {:type  :error/write-value
                    :code  :error
                    :error {:message (ex-data e)}} format))))

(defn unpack
  "after transport, reads the package out from string
 
   (unpack {:format :edn} \"{:type :on/id}\")
   => {:type :on/id}"
  {:added "0.5"}
  [{:keys [format]} message]
  (try
    (read-value message format)
    (catch Exception e
      {:type  :error/read-value
       :code  :error
       :input message
       :error {:message (ex-data e)}})))

(defn random-uuid
  "creates a random uuid as a string"
  {:added "0.5"}
  []
  (str (java.util.UUID/randomUUID)))

(defn now
  "creates a the current time is ms"
  {:added "0.5"}
  []
  (System/currentTimeMillis))

(defn wrap-request
  "creates a function that tracks the request
 
   (defn shortcut [{:keys [pending]} {:keys [tag]}]
     (async/put! (get @pending tag) \"hello\"))
 
   ((wrap-request shortcut)
    {:pending (atom {})
     :return {:type :value}}
    {})
   => \"hello\""
  {:added "0.5"}
  [handler]
  (fn [{:keys [pending return] :as conn}
       {:keys [tag] :as package}]
    (let [tag    (or tag (random-uuid))
          ch     (async/promise-chan)
          _      (if-let [timeout (:timeout return)]
                   (async/go (async/<! (async/timeout timeout))
                             (swap! pending dissoc tag)
                             (async/put! ch
                                         {:type :error/timeout
                                          :code :error
                                          :tag tag
                                          :error {:message (str "request timed out")}})
                             (async/close! ch)))
          _      (swap! pending assoc tag ch)
          result (handler conn (assoc package :code :request :tag tag))]
      (case (:type return)
        :value   (async/<!! ch)
        ch))))

(defn wrap-unpack
  "helper wrapper, uses unpack
 
   ((wrap-unpack (fn [_ msg] msg))
    {:format :edn}
    \"[1 2 3 4]\")
   => [1 2 3 4]
 
   ((wrap-unpack (fn [_ msg] msg))
    {:format :edn}
    \"[1 2 3 4\")
   => {:type :error/read-value
       :code :error
       :input \"[1 2 3 4\"
       :error {:message nil}}"
  {:added "0.5"}
  [handler]
  (fn [conn message]
    (let [package (unpack conn message)]
      (handler conn package))))

(defn dead-fn
  "default function for messages that haven't been processed"
  {:added "0.5"}
  [conn package]
  (println "DEAD PACKAGE:" package))

(defn process-fn
  "default function for processes that
 
   (process-fn {:handlers {:on/id (fn [data] data)}}
               {:type :on/id
                :code :request
                :request \"hello\"})
   => \"hello\""
  {:added "0.5"}
  [{:keys [handlers fn flags] :as conn}
   {:keys [type code] :as package}]
  (let [handler (get handlers type)
        
        data (if (= :full (get flags type))
               package
               (get package code))]
    (cond handler
          (handler data)

          :else
          ((:dead fn) conn data))))

(defn return-fn
  "default function for returning responses
 
   (async/<!! (return-fn {:pending (atom {\"ABCD\" (async/promise-chan)})}
                         {:tag \"ABCD\" :response \"hello\"}))
   => {:tag \"ABCD\", :response \"hello\"}"
  {:added "0.5"}
  [{:keys [pending fn] :as conn}
   {:keys [tag] :as package}]
  (if-let [ch (get @pending tag)]
    (do (swap! pending dissoc tag)
        (doto ch
          (async/put! package)
          (async/close!)))
    ((:dead fn) conn package)))

(defn receive-fn
  "default function for routing received messages
 
   (receive-fn {:fn {:process process-fn}
                :handlers {:on/id (fn [data] data)}}
               {:type :on/id :code :push :push \"hello\"})
   => \"hello\""
  {:added "0.5"}
  [{:keys [fn] :as conn} {:keys [code] :as package}]
  (case code
    :request  ((:respond fn) conn package)
    :response ((:return fn) conn package)
    ((:process fn) conn package)))

(defn wrap-response
  "wrapper for processing messages of code `:request`
 
   ((wrap-response (fn [conn package]
                     (response package)))
    {:fn {:reply (fn [_ package] package)}}
    {:type :on/id :code :request :request \"hello\"})
   => {:type :on/id, :code :response, :request \"hello\"}"
  {:added "0.5"}
  [handler]
  (fn [{:keys [fn] :as conn} package]
    (let [result  (handler conn package)
          response (if (instance? Response result)
                     result
                     {:response result})]
      ((:reply fn) conn (merge package (assoc response :code :response))))))

(defn wrap-time-start
  "wrapper for adding start time
 
   ((wrap-time-start (fn [_ package]
                        package)
                     [:overall])
    nil
    {:params {:time true}})
   => (just-in {:params {:time true},
               :time {:overall {:start number?}}})"
  {:added "0.5"}
  [handler path]
  (fn [conn {:keys [params] :as package}]
    (let [package (if (or (:time params) (:metrics params))
                    (assoc-in package (concat [:time] path) {:start (now)})
                    package)]
      (handler conn package))))

(defn wrap-time-end
  "wrapper for adding start time
 
   ((wrap-time-end (fn [_ package]
                        package)
                     [:overall])
    nil
    {:params {:time true
              :metrics true}
    :time {:overall {:start 0}}})
   => (just-in {:params {:time true
                         :metrics true}
                :time {:overall {:start 0
                                 :end number?}}
                :metrics {:overall number?}})"
  {:added "0.5"}
  [handler path]
  (fn [conn {:keys [params] :as package}]
    (let [tpath   (concat [:time] path)
          package (if-let [start (and (or (:time params)
                                          (:metrics params))
                                      (:start (get-in package tpath)))]
                    (let [end   (now)
                          total (- end start)]
                      (cond-> (update-in package tpath merge {:end end})
                        (:metrics params) (assoc-in (concat [:metrics] path) total)))
                    package)]
      (handler conn package))))

(defn wrap-display
  "wrapper for adding start time
 
   ((wrap-display (fn [_ package]
                    package))
    nil
    {:params {:time false
              :metrics true}
     :time {:overall {:start 0
                     :end 1000}}
     :metrics {:overall 1000}})
   => {:params {:time false
                :metrics true}
       :metrics {:overall 1000}}"
  {:added "0.5"}
  [handler]
  (fn [conn {:keys [params] :as package}]
    (let [package (cond-> package
                    (not (:time params))
                    (dissoc :time))]
      (handler conn package))))

(defn wrap-track
  "wrapper to append a record of the time, id and function
 
   ((wrap-track (fn [_ package]
                  package)
                :sent)
    {:id \"ABCD\"}
    {:params {:track true}})
   => (contains-in {:params {:track true},
                   :track [[:sent \"ABCD\" number?]]})"
  {:added "0.5"}
  [handler code]
  (fn [{:keys [id] :as conn} {:keys [params] :as package}]
    (let [package (if (:track params)
                    (update-in package [:track] (fnil #(conj % [code id (now)]) []))
                    package)]
      (handler conn package))))

(defn init-functions
  "helper function to create the network functions
 
   (init-functions {:fn {:send   (fn [conn package] package)
                         :attach (fn [conn] conn)}})
   => (just-in {:id string?
                :pending #(instance? clojure.lang.Atom %)
                :fn {:attach  fn?
                     :dead    fn?
                    :process fn?
                     :receive fn?
                     :reply   fn?
                     :request fn?
                     :respond fn?
                     :return  fn?
                     :send    fn?}})"
  {:added "0.5"}
  [{:keys [id fn options] :as conn}]
  (let [id         (or id (random-uuid))
        pending    (atom {})
        send-fn    (cond-> (or (:send fn)
                               (throw (ex-info "send function is required" {})))
                     (:track options) (wrap-track :send))
        
        attach-fn  (cond-> (or (:attach fn)
                               (throw (ex-info "attach function is required" {}))))
        
        request-fn (cond-> send-fn
                     (:time options) (wrap-time-start [:overall]) 
                     :then (wrap-request))
        
        process-fn (cond-> (or (:process fn) process-fn))

        reply-fn   (cond-> send-fn
                     (:time options) (wrap-time-end [:remote]))
        
        respond-fn (cond-> process-fn
                     :then (wrap-response)
                     (:time options) (wrap-time-start [:remote]))
        
        return-fn  (cond-> (or (:return fn) return-fn)
                     :then (wrap-display) 
                     (:time options) (wrap-time-end [:overall]))
        
        receive-fn (cond-> (or (:receive fn) receive-fn)
                     (:track options) (wrap-track :receive)
                     :then (wrap-unpack))
        
        dead-fn    (cond-> (or (:dead fn) dead-fn))
        
        conn       (assoc conn
                          :id id
                          :pending pending
                          :fn {:attach  attach-fn
                               :dead    dead-fn
                               :process process-fn
                               :receive receive-fn
                               :reply   reply-fn
                               :request request-fn
                               :respond respond-fn
                               :return  return-fn
                               :send    send-fn})
        _ (attach-fn conn)]
    conn))

(defn request
  "helper function for sending out a request
 
   (def network
     (singleton/singleton
      {:id \"A\"
       :format :edn
       :options {:time  true
                 :track true
                 :network {:delay 100}}
       :default {:params {:full true
                          :metrics true}}
       :return  {:type    :channel
                 :timeout 1000}
      :flags     {:on/id :full}
       :handlers  {:on/id (fn [req] (Thread/sleep 10) (:request req))}}))
   
   (request network :on/id :hello)
   => (just-in {:type :on/id,
                :code :response,
                :request :hello,
                :params {:full true,
                         :metrics true},
                :tag string?
                :response :hello,
                :metrics {:remote number?, :overall number?}})"
  {:added "0.5"}
  ([conn type input]
     (request conn type input {}))
  ([{:keys [return default] :as conn} type input opts]
   (let [ret ((-> conn :fn :request) conn (merge default
                                                 opts
                                                 {:type type
                                                  :code :request
                                                  :request input}))
         ret (if (= (:type return) :value)
               ret
               (async/<!! ret))]
     (cond (= (:code ret) :response)
           (if (-> ret :params :full)
             ret
             (:response ret))
           
           :else
           (throw (ex-info "Invalid response:" ret))))))

(defn message
  "helper function for messaging
 
   (message network :on/id :hello)
 
   (Thread/sleep 500)
   
   (read-string (deref (:raw network)))
   => {:type :on/id
       :params {:full true, :metrics true}
       :code :data,
       :data :hello}"
  {:added "0.5"}
  ([conn type input]
   (message conn type input {}))
  ([{:keys [default] :as conn} type input opts]
   ((-> conn :fn :send) conn (merge default
                                    opts
                                    {:type type
                                     :code :data
                                     :data input}))))

