(ns spirit.network.singleton
  (:require [spirit.protocol.itransport :as transport]
            [spirit.network.common :as common]
            [hara.component :as component]
            [hara.event :as event]))

(defn send-fn
  "send via agent with simulated network delay
 
   (def result (promise))
   
   (send-fn {:raw (doto (agent nil)
                    (add-watch :test (fn [_ _ _ v]
                                       (deliver result v))))
             :format :edn
             :options {:network {:delay 100}}}
            {:type :on/id})
 
   @result
   => \"{:type :on/id}\""
  {:added "0.5"}
  [{:keys [raw options] :as conn} package]
  (let [message (common/pack conn package)]
    (send-off raw (fn [_]
                    (if-let [delay (get-in options [:network :delay])]
                      (Thread/sleep delay))
                    message))
    conn))

(defn attach-fn
  "attaches the `:receive` function to the singleton'
 
   (def raw (agent nil))
   (def result (promise))
   
   (do (attach-fn {:raw raw
                   :fn  {:receive (fn [conn package]
                                    (deliver result package))}})
       (send-off raw (constantly {:type :on/id})))
   @result
   => {:type :on/id}"
  {:added "0.5"}
  [{:keys [raw] :as conn}]
  (let [receive-fn (-> conn :fn :receive)]
    (add-watch raw :receive-fn (fn [_ _ _ package]
                                 (receive-fn conn package))))
  conn)

(defrecord Singleton []

  Object
  (toString [conn]
    (str "#net.singleton" (into {}
                                (-> conn
                                    (dissoc :fn :raw)
                                    (update-in [:pending] (fnil deref (atom {})))))))
  
  component/IComponent
  (-start [conn]
    (-> conn
        (assoc :raw (agent nil)
               :fn  {:send send-fn
                     :attach attach-fn})
        (common/init-functions)))
  (-stop [conn]
    (dissoc conn :pending :raw :fn)))

(defmethod print-method Singleton
  [v w]
  (.write w (str v)))

(defn singleton
  "" [m]
  (-> (map->Singleton m)
      (component/start)))
