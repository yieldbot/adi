(ns spirit.network.common
  (:require [clojure.core.async :as async]))

(defmulti read-value  (fn [string format] format))

(defmulti write-value (fn [data format] format))

(defmethod read-value :edn
  [string _]
  (read-string string))

(defmethod write-value :edn
  [data _]
  (pr-str data))

(defn pack [{:keys [format]}
<<<<<<< Local Changes
            {:keys [type delivery params] :as package}]
=======
            package]
>>>>>>> External Changes
  (try
    (write-value package format)
    (catch Exception e
      (write-value {:type :error/write-value
                    :code :error
                    :error   {:message (ex-data e)}} format))))

(defn unpack [{:keys [format]} message]
  (try
    (read-value message format)
    (catch Exception e
      {:type :error/read-value
       :code :error
       :input   message
       :error   {:message (ex-data e)}} format)))

(defn random-uuid []
  (str (java.util.UUID/randomUUID)))

(defn wrap-request [handler]
  (fn [{:keys [pending return] :as conn}
       {:keys [tag] :as package}]
    (let [tag    (or tag (random-uuid))
          ch     (async/chan)
          _      (if-let [timeout (:timeout return)]
                   (async/go (async/timeout timeout)
                             (println "TIMEOUT")
                             (async/put! ch
                                         {:type :error/timeout
                                          :code :error
                                          :tag tag
                                          :error {:message (str "timeout for " tag)}})
                             (async/close! ch)
                             (swap! pending dissoc tag)))
          _      (swap! pending assoc tag ch)
          result (handler conn (assoc package :code :request :tag tag))]
      (case (:type return)
        :channel ch
        :value   (async/<!! ch)))))

(defn wrap-unpack [handler]
  (fn [conn message]
    (let [package (unpack conn message)]
      (handler conn package))))

(defn dead-fn
  [conn package]
  (println "DEAD PACKAGE:" package))

(defn process-fn
  [{:keys [handlers] :as conn}
   {:keys [type] :as package}]
  ((get handlers type) package))

(defn return-fn
  [{:keys [pending fn] :as conn}
   {:keys [tag response] :as package}]
  (if-let [ch (get @pending tag)]
    (do (async/put! ch package)
        (async/close! ch)
        (swap! pending dissoc tag))
    ((:dead fn) conn package)))

(defn receive-fn [{:keys [fn] :as conn} {:keys [code] :as package}]
  (case code
    :request  ((:respond fn) conn package)
    :response ((:return fn) conn package)
    ((:process fn) conn package)))

(defn wrap-response [handler]
  (fn [{:keys [fn] :as conn} package]
    (let [result (handler conn package)]
      ((:send fn) conn (merge package {:code :response
                                       :response result})))))

(defn init-functions [{:keys [format fn] :as conn}]
  (let [pending    (atom {})
        send-fn    (or (:send fn)
                       (throw (ex-info "send function is required" {})))
        attach-fn  (or (:attach fn)
                       (throw (ex-info "attach function is required" {})))
        
        request-fn (-> send-fn
                       (wrap-request))
        
        process-fn (or (:process fn) process-fn)
        respond-fn (-> process-fn
                       (wrap-response))
        
        return-fn  (or (:return fn) return-fn)
        receive-fn (-> (or (:receive fn) receive-fn)
                       (wrap-unpack))

        dead-fn    (or (:dead fn) dead-fn)
        conn       (assoc conn
                          :pending pending
                          :fn {:attach  attach-fn
                               :dead    dead-fn
                               :process process-fn
                               :receive receive-fn
                               :request request-fn
                               :respond respond-fn
                               :return  return-fn
                               :send    send-fn})
        _ (attach-fn conn receive-fn)]
    conn))
