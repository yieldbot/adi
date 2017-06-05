(ns spirit.common.queue
  (:require [spirit.protocol.iqueue :as interface]))

(def ^:dynamic *default-queue-options*
  {:exclusive false :auto-delete false :durable false})

(def ^:dynamic *default-exchange-options*
  {:type "topic", :internal false, :auto-delete false, :durable true})

(def ^:dynamic *default-binding-options*
  {:routing-key "", :arguments {}, :properties-key "~"})

(defn shorten-topology [topology]
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

(defn lengthen-topology [topology]
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

(defn routing
  ([mq]
   {:queues    (interface/-list-queues mq)
    :exchanges (interface/-list-exchanges mq)
    :bindings  (interface/-list-bindings mq)})
  ([mq {:keys [short]}]
   (let [topology (routing mq)]
     (if short
       (shorten-topology topology)
       topology))))

(defn install-bindings
  [mq bindings]
  (doseq [[source {:keys [queues exchanges]}] (seq bindings)]
    (doseq [[dest bindings] (seq exchanges)]
      (mapv (fn [binding] (interface/-bind-exchange mq source dest binding))
            bindings))
    (doseq [[dest bindings] (seq queues)]
      (mapv (fn [binding] (interface/-bind-queue mq source dest binding))
            bindings))))

(defn install-routing
  ([{:keys [routing] :as mq}]
   (install-routing mq routing))
  ([mq routing]
   (let [{:keys [exchanges queues bindings]} (lengthen-topology routing)]
     (doseq [[name body] (seq exchanges)]
       (interface/-add-exchange mq name body))
     (doseq [[name body] (seq queues)]
       (interface/-add-queue mq name body))
     (install-bindings mq bindings)
     mq)))

(defn remove-routing
  ([{:keys [routing] :as mq}]
   (remove-routing mq routing))
  ([mq routing]
   (let [{:keys [queues exchanges]} (shorten-topology routing)]
     (doseq [name queues]
       (interface/-delete-queue mq name))
     (doseq [name exchanges]
       (interface/-delete-exchange mq name))
     mq)))

(defn purge-routing
  ([mq]
   (let [topology (routing mq)]
     (remove-routing mq topology)
     mq)))


(defn match-pattern [pattern input]
  (-> pattern
      (.replaceAll "\\." "\\\\\\.")
      (.replaceAll "\\*" ".*")
      (re-pattern)
      (re-find input)
      (= input)))

(defn route? [{:keys [type] :as meta}
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

(defrecord MockQueue [state]

  Object
  (toString [mq]
    (str "#" (routing mq {:short {}})))

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
          bind-opts  (merge *default-binding-options*
                            opts
                            {:type :exchange :source source :dest dest})]
      (add-watch source-atm (:id opts)
                 (with-meta
                   (fn [_ _ _ {:keys [meta package] :as data}]
                     (if (route? meta package bind-opts)
                       (swap! dest assoc-in [:package] package)))
                   bind-opts))
      mq))
  
  (-bind-queue      [mq source dest opts]
    (let [source-atm (get-in @state [:exchanges source])
          dest-atm   (get-in @state [:queues dest])
          bind-opts  (merge *default-binding-options*
                            opts
                            {:type :queue :source source :dest dest})]
      (add-watch source-atm (:id opts)
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

  (-add-consumer    [mq name {:keys [id function] :as handler}]
    (-> (get-in @state [:queues name])
        (add-watch id (with-meta (fn [_ _ _ {:keys [package]}]
                                   (function (:body package)))
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
