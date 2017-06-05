(ns spirit.rabbitmq.common
  (:require [spirit.rabbitmq.protocol :as interface]))

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
