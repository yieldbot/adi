(ns spirit.data.exchange.common
  (:require [spirit.protocol.iexchange :as exchange]))

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

(defn routing
  "returns the routes for display
 
   (-> (atom {})
       (routing {:short true}))
   => {:queues #{}, :exchanges #{}, :bindings {}}"
  {:added "0.5"}
  ([mq]
   {:queues    (exchange/-list-queues mq)
    :exchanges (exchange/-list-exchanges mq)
    :bindings  (exchange/-list-bindings mq)})
  ([mq {:keys [short]}]
   (let [topology (routing mq)]
     (if short
       (shorten-topology topology)
       topology))))

(defn install-bindings
  "installs bindings on the mq
   (-> (atom {})
       (exchange/-add-queue \"q1\" {})
       (exchange/-add-exchange \"ex1\" {})
       (install-bindings {\"ex1\" {:queues {\"q1\" [{}]}}})
       (exchange/-list-bindings))
   => (contains-in {\"ex1\" {:queues {\"q1\" [map?]}}})"
  {:added "0.5"}
  [mq bindings]
  (doseq [[source {:keys [queues exchanges]}] (seq bindings)]
    (doseq [[dest bindings] (seq exchanges)]
      (mapv (fn [binding] (exchange/-bind-exchange mq source dest binding))
            bindings))
    (doseq [[dest bindings] (seq queues)]
      (mapv (fn [binding] (exchange/-bind-queue mq source dest binding))
            bindings)))
  mq)

(defn install-routing
  "installs routing on the mq
   (-> (atom {})
       (install-routing routes)
       (routing {:short true}))
   => routes"
  {:added "0.5"}
  ([{:keys [routing] :as mq}]
   (install-routing mq routing))
  ([mq routing]
   (let [{:keys [exchanges queues bindings]} (lengthen-topology routing)]
     (doseq [[name body] (seq exchanges)]
       (exchange/-add-exchange mq name body))
     (doseq [[name body] (seq queues)]
       (exchange/-add-queue mq name body))
     (install-bindings mq bindings)
     mq)))

(defn remove-routing
  "removes routing on the mq
   
   (-> (exchange/create {:type :mock
                         :routing routes})
       (component/start)
       (exchange/-add-queue \"q3\" {})
       (remove-routing)
       (routing {:short true}))
   => {:queues #{\"q3\"}, :exchanges #{}, :bindings {}}"
  {:added "0.5"}
  ([{:keys [routing] :as mq}]
   (remove-routing mq routing))
  ([mq routing]
   (let [{:keys [queues exchanges]} (shorten-topology routing)]
     (doseq [name queues]
       (exchange/-delete-queue mq name))
     (doseq [name exchanges]
       (exchange/-delete-exchange mq name))
     mq)))

(defn purge-routing
  "clears all routing on the mq
   
   (-> (exchange/create {:type :mock
                         :routing routes})
       (component/start)
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
       (exchange/-add-consumer mq queue (assoc handler :id id))))
   mq))
