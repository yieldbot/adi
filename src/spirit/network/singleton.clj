(ns spirit.network.singleton
  (:require [spirit.protocol.itransport :as transport]
            [spirit.network.common :as common]
            [hara.component :as component]
            [hara.event :as event]))

(defn send-fn
  [{:keys [raw] :as conn} package]
  (let [message (common/pack conn package)]
    (send-off raw (fn [_]
                    (if-let [delay (get-in conn [:options :network :delay])]
                      (Thread/sleep delay))
                    message))))

(defn attach-fn
  [{:keys [raw] :as conn} receive-fn]
  (add-watch raw :receive-fn (fn [_ _ _ package]
                               (receive-fn conn package))))

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

(defn singleton [m]
  (-> (map->Singleton m)
      (component/start)))
