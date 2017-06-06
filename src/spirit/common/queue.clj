(ns spirit.common.queue
  (:require [spirit.protocol.iqueue :as interface]))

(def ^:dynamic *default-queue-options*
  {:exclusive false :auto-delete false :durable false})

(def ^:dynamic *default-exchange-options*
  {:type "topic", :internal false, :auto-delete false, :durable true})

(def ^:dynamic *default-binding-options*
  {:routing-key "", :arguments {}, :properties-key "~"})

(defn shorten-topology
  "creates a shorthand version of the routing topology
 
   (shorten-topology (lengthen-topology routes))
   => routes"
  {:added "0.5"}
  [topology]
  (let [set-fn (fn [coll] (if (set? coll) coll (-> coll keys set)))
        bindings-fn (fn [bindings]
                      (reduce-kv (fn [out k binding]
                                   (assoc out k (-> binding
                                                    (update-in [:queues] set-fn)
                                                    (update-in [:exchanges] set-fn))))
                                 {}
                                 bindings))]
    (-> topology
        (update-in [:queues]    set-fn)
        (update-in [:exchanges] set-fn)
        (update-in [:bindings] bindings-fn))))

(defn lengthen-topology
  "display routes in full
 
   (lengthen-topology   {:queues    #{\"q1\" \"q2\"},
                         :exchanges #{\"ex1\" \"ex2\"},
                         :bindings   {\"ex1\" {:exchanges #{\"ex2\"},
                                             :queues #{\"q1\"}}
                                      \"ex2\" {:exchanges #{}
                                             :queues #{\"q2\"}}}})
  => (contains-in
       {:queues {\"q1\" map?
                 \"q2\" map?},
        :exchanges {\"ex1\" map?
                    \"ex2\" map?},
        :bindings {\"ex1\" {:exchanges {\"ex2\" [map?]},
                          :queues {\"q1\" [map?]}},
                   \"ex2\" {:exchanges {},
                          :queues {\"q2\" [map?]}}}})"
  {:added "0.5"}
  [topology]
  (let [set-fn (fn [defaults]
                 (fn [coll]
                   (cond (set? coll)
                         (zipmap coll (repeat defaults))
                         
                         (map? coll)
                         (reduce-kv (fn [out k v]
                                      (assoc out k (merge defaults v)))
                                    {}
                                    coll))))
        set-array-fn (fn [defaults]
                       (fn [coll]
                         (cond (set? coll)
                               (zipmap coll (repeat [defaults]))
                               
                               (map? coll)
                               (reduce-kv (fn [out k v]
                                            (assoc out k (map #(merge defaults %) v)))
                                          {}
                                          coll))))
        bindings-fn (fn [bindings]
                      (reduce-kv (fn [out k binding]
                                   (assoc out k (-> binding
                                                    (update-in [:queues] (set-array-fn *default-binding-options*))
                                                    (update-in [:exchanges] (set-array-fn *default-binding-options*)))))
                                 {}
                                 bindings))]
    (-> topology
        (update-in [:queues]    (set-fn *default-queue-options*))
        (update-in [:exchanges] (set-fn *default-exchange-options*))
        (update-in [:bindings] bindings-fn))))

(defn list-queues
  "returns current list of queues
 
   (list-queues (queue/create {:routing routes}))
   => (contains {\"q1\" map?
                 \"q2\" map?})"
  {:added "0.5"}
  ([mq]
   (interface/-list-queues mq)))

(defn add-queue
  "adds a queue to the mq
 
   (-> (queue/create {:routing routes})
       (add-queue \"q3\")
       (list-queues))
   => (contains {\"q1\" map?
                 \"q2\" map?
                 \"q3\" map?})"
  {:added "0.5"}
  ([mq name]
   (add-queue mq name {}))
  ([mq name opts]
   (interface/-add-queue mq name opts)))

(defn delete-queue
  "deletes a queue from the mq
 
   (-> (queue/create {:routing routes})
       (delete-queue \"q1\")
       (list-queues))
   => (contains {\"q2\" map?})"
  {:added "0.5"}
  ([mq name]
   (interface/-delete-queue mq name)))

(defn list-exchanges
  "returns current list of exchanges
 
   (list-exchanges (queue/create {:routing routes}))
   => (contains {\"ex1\" map?
                 \"ex2\" map?})"
  {:added "0.5"}
  ([mq]
   (interface/-list-exchanges mq)))

(defn add-exchange
  "adds an exchange to the mq
 
   (-> (queue/create {:routing routes})
       (add-exchange \"ex3\")
       (list-exchanges))
   => (contains {\"ex1\" map?
                 \"ex2\" map?
                 \"ex3\" map?})"
  {:added "0.5"}
  ([mq name]
   (add-exchange mq name {}))
  ([mq name opts]
   (interface/-add-exchange mq name opts)))

(defn delete-exchange
  "removes an exchange from the mq
 
   (-> (queue/create {:routing routes})
       (delete-exchange \"ex1\")
       (list-exchanges))
   => (contains {\"ex2\" map?})"
  {:added "0.5"}
  ([mq name]
   (interface/-delete-exchange mq name)))

(defn list-bindings
  "returns current list of exchanges
 
   (list-bindings (queue/create {:routing routes}))
   => (contains-in {\"ex1\" {:exchanges {\"ex2\" [map?]}
                           :queues {\"q1\" [map?]}}
                    \"ex2\" {:queues {\"q2\" [map?]}}})"
  {:added "0.5"}
  ([mq]
   (interface/-list-bindings mq)))

(defn bind-exchange
  "binds a queue to the exchange
 
   (-> (queue/create {:routing routes})
       (add-exchange \"ex3\")
       (bind-exchange \"ex1\" \"ex3\")
       (list-bindings))
   => (contains-in {\"ex1\" {:exchanges {\"ex2\" [map?]
                                       \"ex3\" [map?]}
                          :queues {\"q1\" [map?]}}
                    \"ex2\" {:queues {\"q2\" [map?]}}})"
  {:added "0.5"}
  ([mq source dest]
   (bind-exchange mq source dest {}))
  ([mq source dest opts]
   (interface/-bind-exchange mq source dest opts)))

(defn bind-queue
  "binds an exchange to the exchange
 
   (-> (queue/create {:routing routes})
       (add-queue \"q3\")
       (bind-queue \"ex1\" \"q3\")
       (list-bindings))
   => (contains-in {\"ex1\" {:exchanges {\"ex2\" [map?]}
                           :queues {\"q1\" [map?]
                                   \"q3\" [map?]}}
                    \"ex2\" {:queues {\"q2\" [map?]}}})"
  {:added "0.5"}
  ([mq source dest]
   (bind-queue mq source dest {}))
  ([mq source dest opts]
   (interface/-bind-queue mq source dest opts)))

(defn list-consumers
  "lists all the consumers for the mq
 
   (-> (queue/create {:routing routes :consumers consumers})
       (list-consumers))
   => (contains-in {\"q1\" {:hello map?,
                          :world map?},
                    \"q2\" {:foo map?}})"
  {:added "0.5"}
  ([mq]
   (interface/-list-consumers mq)))

(defn add-consumer
  "adds a consumers to the mq
 
   (-> (queue/create {:routing routes :consumers consumers})
       (add-consumer \"q2\" {:id :bar :sync true :function prn})
       (list-consumers))
   => (contains-in {\"q1\" {:hello map?,
                          :world map?},
                    \"q2\" {:foo map?
                         :bar map?}})"
  {:added "0.5"}
  ([mq name handler]
   (interface/-add-consumer mq name handler)))

(defn delete-consumer
  "deletes the consumer from the queue
   
   (-> (queue/create {:routing routes :consumers consumers})
       (delete-consumer \"q1\" :hello)
       (list-consumers))
   => (contains-in {\"q1\" {:world map?},
                    \"q2\" {:foo map?}})"
  {:added "0.5"}
  ([mq name id]
   (interface/-delete-consumer mq name id)))

(defn publish
  "publishes a message to an exchange
 
   (def p (promise))
   
   (-> (queue/create {:routing routes
                      :consumers {\"q1\" {:hello {:function #(deliver p %)}}}})
       (publish \"ex1\" \"hello there\"))
   
   @p => \"hello there\""
  {:added "0.5"}
  ([mq exchange message]
   (publish mq exchange message {}))
  ([mq exchange message opts]
   (interface/-publish mq exchange message opts)))
 
(defn routing
  "returns the routes for the current mq
   
   (-> (queue/create {:routing routes})
       (routing)
       (shorten-topology))
   => routes"
  {:added "0.5"}
  ([mq]
   {:queues    (list-queues mq)
    :exchanges (list-exchanges mq)
    :bindings  (list-bindings mq)})
  ([mq {:keys [short]}]
   (let [topology (routing mq)]
     (if short
       (shorten-topology topology)
       topology))))

(defn install-bindings
  "installs bindings on the mq
   (-> (queue/create {:routing {:queues #{\"q1\"}
                                :exchanges #{\"ex1\"}}})
       (install-bindings {\"ex1\" {:queues {\"q1\" [{}]}}})
       (list-bindings))
   => (contains-in {\"ex1\" {:queues {\"q1\" [map?]}}})"
  {:added "0.5"}
  [mq bindings]
  (doseq [[source {:keys [queues exchanges]}] (seq bindings)]
    (doseq [[dest bindings] (seq exchanges)]
      (mapv (fn [binding] (bind-exchange mq source dest binding))
            bindings))
    (doseq [[dest bindings] (seq queues)]
      (mapv (fn [binding] (bind-queue mq source dest binding))
            bindings)))
  mq)

(defn install-routing
  "installs routing on the mq
   (-> (queue/create)
       (install-routing routes)
       (routing {:short true}))
   => routes"
  {:added "0.5"}
  ([{:keys [routing] :as mq}]
   (install-routing mq routing))
  ([mq routing]
   (let [{:keys [exchanges queues bindings]} (lengthen-topology routing)]
     (doseq [[name body] (seq exchanges)]
       (add-exchange mq name body))
     (doseq [[name body] (seq queues)]
       (add-queue mq name body))
     (install-bindings mq bindings)
     mq)))

(defn remove-routing
  "removes routing on the mq
   
   (-> (queue/create {:routing routes})
       (add-queue \"q3\")
       (remove-routing)
       (routing {:short true}))
   => {:queues #{\"q3\"}, :exchanges #{}, :bindings {}}"
  {:added "0.5"}
  ([{:keys [routing] :as mq}]
   (remove-routing mq routing))
  ([mq routing]
   (let [{:keys [queues exchanges]} (shorten-topology routing)]
     (doseq [name queues]
       (delete-queue mq name))
     (doseq [name exchanges]
       (delete-exchange mq name))
     mq)))

(defn purge-routing
  "clears all routing on the mq
   
   (-> (queue/create {:routing routes})
       (purge-routing)
       (routing {:short true}))
   => {:queues #{}, :exchanges #{}, :bindings {}}"
  {:added "0.5"}
  ([mq]
   (let [topology (routing mq)]
     (remove-routing mq topology)
     mq)))

(defn install-consumers
  "installs-consumers on the queues"
  {:added "0.5"}
  ([{:keys [consumers] :as mq}]
   (install-consumers mq consumers))
  ([mq consumers]
   (doseq [[queue handlers] (seq consumers)]
     (doseq [[id handler] (seq handlers)]
       (add-consumer mq queue (assoc handler :id id))))
   mq))

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
    (str "#mq" (routing mq {:short {}})))

  interface/IQueue
  (-list-queues     [mq]
    (->> @state
         :queues
         (reduce-kv (fn [out k atm]
                      (assoc out k (:meta @atm)))
                    {})))
  
  (-add-queue       [mq name opts]
    (swap! state update-in [:queues] assoc name
           (atom {:meta (merge *default-queue-options* opts)}))
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
           (atom {:meta (merge *default-exchange-options* opts)}))
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
          bind-opts  (merge *default-binding-options*
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
          bind-opts  (merge *default-binding-options*
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
    mq))

(defmethod print-method MockMQ
  [v w]
  (.write w (str v)))

(defmulti create-queue
  ""
  :type)

(defmethod create-queue :mock
  ([m]
   (merge (MockMQ. (atom {}))
          m)))

(defn create
  ""
  ([] (create {}))
  ([{:keys [type routing consumers refresh] :as m}]
   (let [type (or type :mock)
         mq (create-queue (assoc m :type type))]
     (cond-> mq
       refresh   (purge-routing)
       routing   (install-routing routing)
       consumers (install-consumers consumers)))))


