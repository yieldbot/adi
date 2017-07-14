(ns spirit.data.exchange.base
  (:require [spirit.protocol.iexchange :as exchange]
            [spirit.data.exchange.common :as common]
            [hara.component :as component]))

(defn match-pattern
  "creates a re-pattern for the rabbitmq regex string
 
   (match-pattern \"*\" \"hello\")
   => true
 
   (match-pattern \".*.\" \".hello.\")
   => true"
  {:added "0.5"}
  [pattern input]
  (-> pattern
      (.replaceAll "\\." "\\\\\\.")
      (.replaceAll "\\*" ".*")
      (re-pattern)
      (re-find input)
      (= input)))

(defn route?
  "checks if a message will be routed
 
   (route? {:type \"fanout\"} {} {})
   => true
 
   (route? {:type \"topic\"}
           {:key \"user.account.login\"}
           {:routing-key \"*.account.*\"})
   => true
 
   (route? {:type \"header\"}
           {:headers {\"account\" \"login\"}}
           {:arguments {\"account\" \"*\"}})
   => true"
  {:added "0.5"}
  [{:keys [type] :as meta}
   {:keys [key headers] :as package}
   {:keys [routing-key arguments] :as bind-opts}]
  (cond (= type "fanout")
        true
        
        (= type "direct")
        (throw (Exception. "DIRECT ROUTING NOT IMPLEMENTED."))
        
        (= type "topic")
        (cond (and (empty? routing-key)
                   (empty? key))
              true
              
              :else
              (match-pattern routing-key key))
        
        (= type "header")
        (every? (fn [[k pattern]]
                  (match-pattern pattern (get headers k)))
                (seq arguments))))

(defrecord MockMQ [state]

  Object
  (toString [mq]
    (str "#mq" (common/routing mq {:short {}})))

  exchange/IExchange
  (-list-queues     [mq]
    (->> @state
         :queues
         (reduce-kv (fn [out k atm]
                      (assoc out k (:meta @atm)))
                    {})))
  
  (-add-queue       [mq name opts]
    (swap! state update-in [:queues] assoc name
           (atom {:meta (merge common/*default-queue-options* opts)}))
    mq)
  
  (-delete-queue    [mq name]
    (swap! state update-in [:queues] dissoc name)
    mq)
  
  (-list-exchanges  [mq]
    (->> @state
         :exchanges
         (reduce-kv (fn [out k atm]
                      (assoc out k (:meta @atm)))
                    {})))
  
  (-add-exchange    [mq name opts]
    (swap! state update-in [:exchanges] assoc name
           (atom {:meta (merge common/*default-exchange-options* opts)}))
    mq)
  
  (-delete-exchange [mq name]
    (swap! state update-in [:exchanges] dissoc name)
    mq)
  
  (-list-bindings   [mq]
    (->> @state :exchanges
         (reduce-kv (fn [out k atm]
                      (let [watches (.getWatches atm)]
                        (reduce-kv (fn [out bind watch]
                                     (let [{:keys [type dest] :as opt} (meta watch)
                                           type (keyword (str (name type) "s"))
                                           nopt (dissoc opt :type :source :dest)]
                                       (update-in out [k type dest] (fnil #(conj % nopt) []))))
                                   out
                                   watches)))
                    {})))
  
  (-bind-exchange   [mq source dest opts]
    (let [source-atm (get-in @state [:exchanges source])
          dest-atm   (get-in @state [:exchanges dest])
          id         (or (:id opts) (str (java.util.UUID/randomUUID)))
          bind-opts  (merge common/*default-binding-options*
                            opts
                            {:id id :type :exchange :source source :dest dest})]
      (add-watch source-atm id
                 (with-meta
                   (fn [_ _ _ {:keys [meta package] :as data}]
                     (if (route? meta package bind-opts)
                       (swap! dest-atm assoc-in [:package] package)))
                   bind-opts))
      mq))
  
  (-bind-queue      [mq source dest opts]
    (let [source-atm (get-in @state [:exchanges source])
          dest-atm   (get-in @state [:queues dest])
          id         (or (:id opts) (str (java.util.UUID/randomUUID)))
          bind-opts  (merge common/*default-binding-options*
                            opts
                            {:id id :type :queue :source source :dest dest})]
      (add-watch source-atm id
                 (with-meta
                   (fn [_ _ _ {:keys [meta package] :as data}]
                     (if (route? meta package bind-opts)
                       (swap! dest-atm assoc-in [:package] package)))
                   bind-opts))
      mq))

  (-list-consumers  [mq]
    (->> (:queues @state)
         (reduce-kv (fn [out k v]
                      (assoc out k (->> (.getWatches v)
                                        (reduce-kv (fn [out k v]
                                                     (assoc out k (meta v)))
                                                   {}))))
                    {})))

  (-add-consumer    [mq name {:keys [id function sync] :as handler}]
    (-> (get-in @state [:queues name])
        (add-watch id (with-meta (fn [_ _ _ {:keys [package]}]
                                   (if sync
                                     (function (:body package))
                                     (future (function (:body package)))))
                        handler)))
    mq)
  
  (-delete-consumer [mq name id]
    (-> (get-in @state [:queues name])
        (remove-watch id))
    mq)
  
  (-publish         [mq exchange body {:keys [key headers]}]
    (let [ex-atm (get-in @state [:exchanges exchange])]
      (swap! ex-atm assoc :package {:key key :headers headers :body body}))
    mq)

  component/IComponent

  (-start [{:keys [routing consumers refresh] :as mq}]
    (cond-> mq
       refresh   (common/purge-routing)
       routing   (common/install-routing routing)
       consumers (common/install-consumers consumers)))

  (-stop [mq] mq))

(defmethod print-method MockMQ
  [v w]
  (.write w (str v)))

(defmethod exchange/create :mock
  ([m]
   (merge (MockMQ. (atom {}))
          m)))
