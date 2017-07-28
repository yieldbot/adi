(ns spirit.network.base.endpoint
  (:require [spirit.network.common :as common]
            [clojure.core.async :as async]
            [hara.component :as component]))

(defn send-fn
  "default send function for active endpoint
 
   (def ch (async/promise-chan))
   
   (send-fn {:raw (atom {:out ch})} {:hello :there})
 
   (async/<!! ch)
   => \"{:hello :there}\""
  {:added "0.5"}
  [{:keys [raw options] :as conn} package]
  (let [message (common/pack conn package)]
    (async/put! (:out @raw) message)
    conn))

(defn attach-fn
  "default attach function for active endpoint
 
   (def result (promise))
   (def receive-fn (fn [conn package]
                     (deliver result package)))
   (def ch (async/chan))
   (def conn {:raw (atom {:in ch})
              :fn  {:receive receive-fn
                    :close close-fn}})
   
   (do (attach-fn conn)
       (async/put! ch {:hello :there})
       
       (deref result))
   => {:hello :there}
   
   (do (async/close! ch)
       (Thread/sleep 100)
       (deref (:raw conn)))
   => nil"
  {:added "0.5"}
  [{:keys [raw fn] :as conn}]
  (let [receive-fn (:receive fn)]
    (async/go-loop []
      (if-let [message (and (:in @raw)
                            (async/<! (:in @raw)))]
        (do (receive-fn conn message)
            (recur))
        ((:close fn) conn))))
  conn)

(defn close-fn
  "default close function for active endpoint
 
   (def conn {:raw (atom {:in  (async/chan)
                          :out (async/chan)})})
 
   (close-fn conn)
   (deref (:raw conn))
   => nil"
  {:added "0.5"}
  [{:keys [raw] :as conn}]
  (let [{:keys [in out]} @raw]
    (reset! raw nil)
    (if in  (async/close! in))
    (if out (async/close! out))))

(defn active?-fn
  "default check for active endpoint
 
   (def conn {:raw (atom {:in  (async/chan)
                          :out (async/chan)})})
 
   (active?-fn conn)
   => true
 
   (close-fn conn)
   (active?-fn conn)
   => false"
  {:added "0.5"}
  [{:keys [raw] :as conn}]
  (boolean @raw))

(defrecord Endpoint []

  Object
  (toString [conn]
    (str "#net.endpoint" (into {} (apply dissoc conn (:hide conn)))))
  
  component/IComponent
  (-start [{:keys [hide raw] :as conn}]
    (-> conn
        (assoc :hide (or hide [:fn :raw :handlers :pending :hide])
               :raw  (or raw (atom nil)))
        (update-in [:fn] (partial merge
                                  {:active? active?-fn
                                   :attach  attach-fn
                                   :close   close-fn
                                   :send    send-fn}))
        (common/init-functions)))
  
  (-stop [{:keys [raw hub fn] :as conn}]
    ((:close fn) conn)
    (dissoc conn :pending :fn)))

(defmethod print-method Endpoint
  [v w]
  (.write w (str v)))

(defn endpoint
  "creates an endpoint
   
   (def a (endpoint {:id \"A\"
                     :format   :edn
                     :options  {:time true :track true}
                    :default  {:params {:full true :metrics true}}
                     :return   {:type :channel :timeout 1000}
                     :handlers {:on/id (fn [req] (Thread/sleep 100) :a)}}))"
  {:added "0.5"}
  [m]
  (-> (map->Endpoint m)
      (component/start)))

(defn connect
  "connects a pair of endpoints together
 
   (def b (endpoint {:id \"B\"
                     :format   :edn
                     :options  {:time true :track true}
                     :default  {:params {:full true :metrics true}}
                     :return   {:type :channel :timeout 1000}
                     :handlers {:on/id (fn [req] (Thread/sleep 150) :b)}}))
   
   (connect a b)
   
   (common/request a :on/id {} {:params {:metrics true :full true}})
   => (contains-in {:type :on/id
                    :params {:metrics true
                             :full true}
                    :code :response
                    :request {}
                    :tag string?
                    :response :b
                    :metrics {:remote  number?
                              :overall number?}})
 
 
   (common/request a :on/oeuoeu {})
   => (contains {:type :on/oeuoeu
                 :status :error
                :code :response})"
  {:added "0.5"}
  [a b]
  (let [a->b (async/chan)
        b->a (async/chan)
        _    (reset! (:raw a) {:in  b->a :out a->b})
        _    (reset! (:raw b) {:in  a->b :out b->a})]
    ((-> a :fn :attach) a)
    ((-> b :fn :attach) b)
    [a b]))

(defn coupled
  "returns a pair of connected endpoints
   
   (def duo (coupled {:id \"A\"
                      :format   :edn
                      :return   {:type :value}
                      :handlers {:on/id (fn [req] (Thread/sleep 100) :a)}}
                     {:id \"B\"
                      :format   :edn
                      :return   {:type :value}
                      :handlers {:on/id (fn [req] (Thread/sleep 100) :b)}}))
   
   (common/request (first duo) :on/id nil)
   => :b
 
   (common/request (second duo) :on/id nil)
   => :a"
  {:added "0.5"}
  [ma mb]
  (let [a (endpoint ma)
        b (endpoint mb)]
    (connect a b)))
