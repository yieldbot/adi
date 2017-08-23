(ns spirit.data.exchange.base
  (:require [spirit.protocol.iexchange :as exchange]
            [spirit.data.exchange.common :as common]
            [spirit.data.atom :as atom]
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

(defn add-in-atom
  "helper for adding queues and exchanges"
  {:added "0.5"}
  [atom keyword name opts defaults]
  (doto atom
    (swap! update-in [keyword] assoc name
           (clojure.core/atom {:meta (merge defaults opts)}))))

(defn add-exchange-atom
  "adds a queue to the atom
 
   (-> (atom {})
       (add-exchange-atom \"ex1\" {})
       (add-exchange-atom \"ex2\" {}) 
       ((comp sort keys :exchanges deref)))
   => [\"ex1\" \"ex2\"]"
  {:added "0.5"}
  [atom name opts]
  (add-in-atom atom :exchanges name opts common/*default-exchange-options*))

(defn add-queue-atom
  "adds a queue to the atom
 
   (-> (atom {})
       (add-queue-atom \"q3\" {})
       (add-queue-atom \"q2\" {}) 
       ((comp sort keys :queues deref)))
   => [\"q2\" \"q3\"]"
  {:added "0.5"}
  [atom name opts]
  (add-in-atom atom :queues name opts common/*default-queue-options*))

(defn list-in-atom
  "returns current list of queues
   (-> (atom {})
       (add-queue-atom \"q3\" {}) 
       (list-in-atom :queues))
   => {\"q3\" {:exclusive false
             :auto-delete false
             :durable false}}"
  {:added "0.5"}
  [atom keyword]
  (->> (keyword @atom)
       (reduce-kv (fn [out k atom]
                    (assoc out k (:meta @atom)))
                  {})))

(defn delete-in-atom
  "returns current list of queues
   (-> (atom {})
       (add-exchange-atom \"ex1\" {})
       (add-exchange-atom \"ex2\" {})
       (delete-in-atom :exchanges \"ex1\")
       (list-in-atom :exchanges))
   => {\"ex2\" {:type \"topic\"
              :internal false
             :auto-delete false
              :durable true}}"
  {:added "0.5"}
  [atom keyword name]
  (doto atom
    (swap! update-in [keyword] dissoc name)))

(defn list-bindings-atom
  "returns current list of exchanges
 
   (-> (atom {})
       (common/install-routing routes)
       (list-bindings-atom))
   => (contains-in {\"ex1\" {:exchanges {\"ex2\" [map?]}
                           :queues {\"q1\" [map?]}}
                    \"ex2\" {:queues {\"q2\" [map?]}}})"
  {:added "0.5"}
  [atom]
  (->> (:exchanges @atom)
       (reduce-kv (fn [out k atom]
                    (let [watches (.getWatches atom)]
                      (reduce-kv (fn [out bind watch]
                                   (let [{:keys [type dest] :as opt} (meta watch)
                                         type (keyword (str (name type) "s"))
                                         nopt (dissoc opt :type :source :dest)]
                                     (update-in out [k type dest] (fnil #(conj % nopt) []))))
                                 out
                                 watches)))
                  {})))

(defn bind-in-atom
  "helper function for binding to queues and exchanges"
  {:added "0.5"}
  [atom [source-key dest-key bind-type] source dest opts]
  (let [source-atom (get-in @atom [source-key source])
        dest-atom   (get-in @atom [dest-key dest])
        id          (or (:id opts) (str (java.util.UUID/randomUUID)))
        bind-opts   (merge common/*default-binding-options*
                           opts
                           {:id id :type bind-type :source source :dest dest})]
    (add-watch source-atom id
               (with-meta
                 (fn [_ _ _ {:keys [meta package] :as data}]
                   (if (route? meta package bind-opts)
                     (swap! dest-atom assoc-in [:package] package)))
                 bind-opts))
    atom))

(defn bind-exchange-atom
  "binds a queue to the exchange
 
   (-> (atom {})
       (common/install-routing routes)
       (add-exchange-atom \"ex3\" {})
       (bind-exchange-atom \"ex1\" \"ex3\" {})
       (list-bindings-atom))
   => (contains-in {\"ex1\" {:exchanges {\"ex2\" [map?]
                                      \"ex3\" [map?]}
                           :queues {\"q1\" [map?]}}
                    \"ex2\" {:queues {\"q2\" [map?]}}})"
  {:added "0.5"}
  [atom source dest opts]
  (bind-in-atom atom [:exchanges :exchanges :exchange] source dest opts))

(defn bind-queue-atom
  "binds an exchange to a queue
 
   (-> (atom {})
       (common/install-routing routes)
       (add-queue-atom \"q3\" {})
       (bind-queue-atom \"ex1\" \"q3\" {})
       (list-bindings-atom))
   => (contains-in {\"ex1\" {:exchanges {\"ex2\" [map?]}
                          :queues {\"q1\" [map?]
                                    \"q3\" [map?]}}
                    \"ex2\" {:queues {\"q2\" [map?]}}})"
  {:added "0.5"}
  [atom source dest opts]
  (bind-in-atom atom [:exchanges :queues :queue] source dest opts))

(defn list-consumers-atom
  "lists all connected consumers
 
   (-> (atom {})
       (common/install-routing routes)
       (add-consumer-atom  \"q2\" {:id :bar
                                 :sync true
                                 :function prn})
       (list-consumers-atom))
  => (contains-in {\"q1\" {}
                    \"q2\" {:bar {:id :bar,
                                :sync true,
                                :function fn?}}})"
  {:added "0.5"}
  [atom]
  (->> (:queues @atom)
       (reduce-kv (fn [out k v]
                    (assoc out k (->> (.getWatches v)
                                      (reduce-kv (fn [out k v]
                                                   (assoc out k (meta v)))
                                                 {}))))
                  {})))

(defn add-consumer-atom
  "adds a consumer to the queue"
  {:added "0.5"}
  [atom name {:keys [id function sync] :as handler}]
  (-> (get-in @atom [:queues name])
      (add-watch id (with-meta (fn [_ _ _ {:keys [package]}]
                                 (if sync
                                   (function (:body package))
                                   (future (function (:body package)))))
                      handler)))
  atom)

(defn delete-consumer-atom
  "deletes a consumer to the queue"
  {:added "0.5"}
  [atom name id]
  (-> (get-in @atom [:queues name])
      (remove-watch id))
  atom)

(defn publish-atom
  "publishes a message to the exchange
 
   (def p (promise))
   
   (-> (atom {})
       (common/install-routing routes)
       (add-consumer-atom \"q1\" {:id :bar
                                :sync true
                                :function #(deliver p %)})
       (publish-atom \"ex1\" \"hello there\" {}))
   
   @p => \"hello there\""
  {:added "0.5"}
  [atom exchange body {:keys [key headers] :as opts}]
  (let [exchange-atom (get-in @atom [:exchanges exchange])]
    (swap! exchange-atom assoc :package {:key key :headers headers :body body})
    atom))

(extend-protocol exchange/IExchange
  clojure.lang.Atom
  (-list-queues      [atom] (list-in-atom atom :queues))
  (-add-queue        [atom name opts] (add-queue-atom atom name opts))
  (-delete-queue     [atom name] (delete-in-atom atom :queues name))
  (-list-exchanges   [atom] (list-in-atom atom :exchanges))
  (-add-exchange     [atom name opts] (add-exchange-atom atom name opts))
  (-delete-exchange  [atom name] (delete-in-atom atom :exchanges name))
  (-list-bindings    [atom] (list-bindings-atom atom))
  (-bind-exchange    [atom source dest opts] (bind-exchange-atom atom source dest opts))
  (-bind-queue       [atom source dest opts] (bind-queue-atom atom source dest opts))
  (-list-consumers   [atom] (list-consumers-atom atom))
  (-add-consumer     [atom name handler] (add-consumer-atom atom name handler))
  (-delete-consumer  [atom name id] (delete-consumer-atom atom name id))
  (-publish          [atom exchange body opts] (publish-atom atom exchange body opts)))

(defrecord MockExchange [state]

  Object
  (toString [mq] (str "#exchange.mock"
                      (if state
                        (common/routing state {:short {}})
                        "<uninitiased>")))
  
  exchange/IExchange
  (-list-queues      [mq] (exchange/-list-queues state))
  (-add-queue        [mq name opts] (exchange/-add-queue state name opts) mq)
  (-delete-queue     [mq name] (exchange/-delete-queue state name) mq)
  (-list-exchanges   [mq] (exchange/-list-exchanges state))
  (-add-exchange     [mq name opts] (exchange/-add-exchange state name opts) mq)
  (-delete-exchange  [mq name] (exchange/-delete-exchange state name) mq)
  (-list-bindings    [mq] (exchange/-list-bindings state))
  (-bind-exchange    [mq source dest opts] (exchange/-bind-exchange state source dest opts) mq)
  (-bind-queue       [mq source dest opts] (exchange/-bind-queue state source dest opts) mq)
  (-list-consumers   [mq] (exchange/-list-consumers state))
  (-add-consumer     [mq name handler] (exchange/-add-consumer state name handler) mq)
  (-delete-consumer  [mq name id] (exchange/-delete-consumer state name id) mq)
  (-publish          [mq exchange body opts] (exchange/-publish state exchange body opts) mq)
  
  component/IComponent
  (-start [{:keys [routing consumers refresh file log] :as mq}]
    (cond-> mq
      file      (assoc-in [:file :transform] (comp common/routing atom))
      log       (assoc-in [:log :transform] (comp common/routing atom))
      :then     (atom/attach-state)
      refresh   (common/purge-routing)
      routing   (common/install-routing routing)
      consumers (common/install-consumers consumers)))
  
  (-stop [mq]
    (atom/detach-state mq)))

(defmethod print-method MockExchange
  [v w]
  (.write w (str v)))

(defmethod exchange/create :mock
  [m]
  (map->MockExchange m))
