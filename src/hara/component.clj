(ns hara.component
  (:require [hara.common.checks :refer [hash-map?] :as checks]
            [hara.data.map :refer [assoc-if]]
            [hara.data.nested :refer [merge-nested]]
            [hara.data.record :as record]
            [hara.sort.topological :refer [topological-sort]]
            [clojure.set :as set]))

(defprotocol IComponent
  (-start [component])
  (-stop  [component])
  (-started? [component])
  (-stopped? [component])
  (-properties [component]))

(extend-protocol IComponent
  Object
  (-start [this] this)
  (-stop  [this] this)
  (-properties [this] {}))

(defn iobj? [x]
  (instance? clojure.lang.IObj x))

(defn primitive? [x]
  (or (string? x)
      (number? x)
      (checks/boolean? x)
      (checks/regex? x)
      (checks/uuid? x)
      (checks/uri? x)
      (checks/url? x)))

(defn component?
  "checks if an instance extends IComponent
 
   (component? (Database.))
   => true"
  {:added "2.2"}
  [x]
  (extends? IComponent (type x)))

(defn started?
  "checks if a component has been started
 
   (started? (Database.))
   => false
 
   (started? (start (Database.)))
   => true
 
   (started? (stop (start (Database.))))
   => false"
  {:added "2.1"}
  [component]
  (try (-started? component)
       (catch IllegalArgumentException e
         (if (iobj? component)
           (-> component meta :started true?)
           (primitive? component)))
       (catch AbstractMethodError e
         (if (iobj? component)
           (-> component meta :started true?)
           (primitive? component)))))

(defn stopped?
  "checks if a component has been stopped
 
   (stopped? (Database.))
   => true
 
   (stopped? (start (Database.)))
   => false
 
   (stopped? (stop (start (Database.))))
   => true"
  {:added "2.1"}
  [component]
  (try (-stopped? component)
       (catch IllegalArgumentException e
         (-> component started? not))
       (catch AbstractMethodError e
         (-> component started? not))))

(defn start
  "starts a component/array/system
 
   (start (Database.))
   => (just {:status \"started\"})"
  {:added "2.1"}
  [component]
  (let [cp (-start component)]
    (if (iobj? cp)
      (vary-meta cp assoc :started true)
      cp)))

(defn stop
  "stops a component/array/system
 
   (stop (start (Database.)))
   => (just {})"
  {:added "2.1"}
  [component]
  (let [cp (-stop component)]
    (if (iobj? cp)
      (vary-meta cp dissoc :started)
      cp)))

(defn properties
  [component]
  (try (-properties component)
       (catch IllegalArgumentException e
         {})
       (catch AbstractMethodError e
         {})))

(declare system? array? start-array stop-array)

(deftype ComponentArray [arr]
  Object
  (toString [this]
    (let [{:keys [tag display]} (meta this)]
      (str "#"
           (or tag "arr")
           (if display
             (display this)
             (mapv (fn [v]
                     (cond (or (system? v)
                               (array? v)
                               (not (component? v)))
                           v

                           :else
                           (reduce (fn [m [k v]]
                                     (cond (extends? IComponent (type v)) ;; for displaying internal keys
                                           (update-in m ['*] (fnil #(conj % k) []))

                                           :else
                                           (assoc m k v)))
                                   (record/empty v)
                                   v)))
                   arr)))))

  IComponent
  (-start [this] (start-array this))
  (-stop  [this] (stop-array  this))

  clojure.lang.Seqable
  (seq [this] (seq arr))

  clojure.lang.IObj
  (withMeta [this m]
    (ComponentArray. (with-meta arr m)))

  clojure.lang.IMeta
  (meta [this] (meta arr))

  clojure.lang.Counted
  (count [this] (count arr))

  clojure.lang.Indexed
  (nth [this i]
    (nth arr i nil))

  (nth [ova i not-found]
    (nth arr i not-found)))

(defmethod print-method ComponentArray
  [v ^java.io.Writer w]
  (.write w (str v)))

(defn start-array
  ""
  [carr]
  (with-meta
    (ComponentArray. (mapv start (seq carr)))
    (meta carr)))

(defn stop-array
  ""
  [carr]
  (with-meta
    (ComponentArray. (mapv stop (seq carr)))
    (meta carr)))

(defn constructor [x]
  (if (map? x)
    (:constructor x)
    x))

(defn array
  "creates an array of components
 
   (def recs (start (array map->Database [{:id 1} {:id 2}])))
   (count (seq recs)) => 2
   (first recs) => (just {:id 1 :status \"started\"})"
  {:added "2.1"}
  [{:keys [constructor]} config]
  (if (vector? config)
    (let [defaults (meta config)]
      (ComponentArray. (mapv (fn [entry]
                               (if (map? entry)
                                 (constructor (merge-nested defaults entry))
                                 entry))
                             config)))
    (throw (Exception. (str "Config " config " has to be a vector.")))))

(defn array?
  "checks if object is a component array
 
   (array? (array map->Database []))
   => true"
  {:added "2.1"}
  [x]
  (instance? ComponentArray x))

(declare start-system stop-system)

(defn system-string
  ""
  ([sys]
   (let [{:keys [tag display]} (meta sys)]
     (str "#" (or tag "sys") " "
          (if display
            (display sys)
            (reduce (fn [m [k v]]
                      (cond (or (system? v)
                                (array? v)
                                (not (component? v)))
                            (assoc m k v)
                            
                            :else
                            (assoc m k (reduce (fn [m [k v]]
                                                 (cond (extends? IComponent (type v))
                                                       (update-in m ['*] (fnil #(conj % k) []))
                                                       
                                                       :else
                                                       (assoc m k v)))
                                               (record/empty v)
                                               v))))
                    {} sys))))))

(defrecord ComponentSystem []
  Object
  (toString [sys]
    (system-string sys))

  IComponent
  (-start [sys]
    (start-system sys))
  (-stop [sys]
    (stop-system sys)))

(defmethod print-method ComponentSystem
  [v ^java.io.Writer w]
  (.write w (str v)))

(defn system?
  "checks if object is a component system
 
   (system? (system {} {}))
   => true"
  {:added "2.1"}
  [x]
  (instance? ComponentSystem x))

(defn long-form-imports [args]
    (->> args
         (map (fn [x]
                (cond (keyword? x)
                      [x {:type :single :as x}]
                      (vector? x)
                      [(first x) (merge {:type :single} (second x))])))
         (into {})))

(defn long-form-entry [[desc & args]]
  (let [dependencies  (map (fn [x] (if (vector? x)
                                     (first x)
                                     x))
                           args)
        [desc form] (if (vector? desc)
                      [(first desc) {:compile :array}]
                      [desc {:compile :single}])
        desc (cond (fn? desc)
                   {:type :build :constructor desc}

                   (:type desc) desc
                   
                   (:expose desc)
                   (-> desc
                       (dissoc :expose)
                       (assoc :type :expose :in (first dependencies) :function (:expose desc)))

                   :else
                   (assoc desc :type :build))]
    (cond-> (merge form desc)
      (= :build (:type desc))
      (assoc :import (long-form-imports args))

      :finally
      (assoc :dependencies dependencies))))

(defn long-form [topology]
    (reduce-kv (fn [m k v]
                 (assoc m k (long-form-entry v)))
               {} topology))

(defn get-dependencies [full-topology]
  (reduce-kv (fn [m k v]
               (assoc m k (set (:dependencies v))))
             {} full-topology))

(defn get-exposed [full-topology]
  (reduce-kv (fn [arr k v]
               (if (= :expose (:type v))
                 (conj arr k)
                 arr))
             [] full-topology))

(defn all-dependencies [m]
  (let [order (topological-sort m)]
    (reduce (fn [out key]
              (let [inputs (set (get m key))
                    result (set (concat inputs (mapcat out inputs)))]
                (assoc out
                       key
                       result)))
            {}
            order)))

(defn valid-subcomponents [full-topology keys]
    (let [expose-keys (get-exposed full-topology)
          valid-keys (set (concat expose-keys keys))
          sub-keys (->> full-topology
                        get-dependencies
                        all-dependencies
                        (reduce-kv (fn [m k v]
                                     (assoc m k (conj v k)))
                                   {}))]
      
      (reduce-kv (fn [arr k v]
                   (if (set/superset? valid-keys v)
                     (conj arr k)
                     arr))
                 []
                 sub-keys)))

(defn system
  ([topology config]
   (system topology config {:partial false}))
  ([topology config {:keys [partial? tag display] :as opts}]
   (let [full   (long-form topology)
         valid  (valid-subcomponents full (keys config))
         expose (get-exposed full)
         diff  (set/difference (set (keys full)) valid)
         _     (or (empty? diff)
                   partial?
                   (throw (Exception. (str "Missing Config Keys: " diff))))
         build    (apply dissoc full diff)
         dependencies (apply dissoc (get-dependencies full) diff)
         order (topological-sort dependencies)
         initial  (apply dissoc build (concat diff (get-exposed full)))]
     (-> (reduce-kv (fn [sys k {:keys [constructor compile] :as build}]
                      
                      (let [cfg (get config k)]
                        (assoc sys k (cond (= compile :array)
                                           (array build cfg)
                                           
                                           :else
                                           (constructor cfg)))))
                    (ComponentSystem.)
                    initial)
         (with-meta (merge {:partial (not (empty? diff))
                            :build   build
                            :order   order
                            :dependencies dependencies}
                           opts))))))

(defn system-import [component system import]
  (reduce-kv (fn [out k v]
               (let [{:keys [type as]} (get import k)
                     subsystem (get system k)]
                 (cond (array? out)
                       (cond->> (seq out)
                         (= type :element)
                         (map #(assoc %2 as %1) subsystem)
                         
                         (= type :single)
                         (map #(assoc % as subsystem))
                         
                         :finally
                         (ComponentArray.))
                       
                       :else
                       (assoc out as subsystem))))
             component
             import))

(defn system-expose [_ system {:keys [in function] :as opts}]
  (let [subsystem (get system in)]
    (cond (array? subsystem)
          (->> (sequence subsystem)
               (map function)
               (ComponentArray.))
          
          :else
          (function subsystem))))

(defn perform-triggers [component functions ks]
  (reduce (fn [out k]
            (let [func (or (get functions k)
                           identity)]
              (func out)))
          component
          ks))

(defn start-system [system]
  (let [{:keys [build order] :as meta} (meta system)]
    (reduce (fn [out k]
              (let [component (get out k)
                    {:keys [type import setup triggers functions] :as opts} (get build k)
                    {:keys [pre-start post-start] :or
                     {pre-start [] post-start []}} triggers
                    setup (or setup identity)
                    result (cond-> (perform-triggers component functions pre-start)
                             (= type :build)
                             (system-import out import)
                             
                             (= type :expose)
                             (system-expose out opts)
                             
                             :finally
                             (-> start setup (perform-triggers functions post-start)))]
                (assoc out k result)))
            system
            order)))

(defn system-deport [component import]
  (reduce-kv (fn [out k v]
               (let [{:keys [type as]} (get import k)]
                 (cond (array? out)
                       (->> (seq out)
                            (map #(dissoc % as))
                            (ComponentArray.))
                       
                       :else
                       (dissoc out as))))
             component
             import))

(defn stop-system [system]
  (let [{:keys [build order] :as meta} (meta system)]
    (reduce (fn [out k]
              (let [component (get out k)
                    {:keys [type import teardown triggers functions] :as opts} (get build k)
                    {:keys [pre-stop post-stop] :or
                     {pre-stop [] post-stop []}} triggers
                    teardown (or teardown identity)
                    component (-> component
                                  (perform-triggers functions pre-stop)
                                  (teardown)
                                  (stop)
                                  (perform-triggers functions post-stop))]
                (cond (= type :build)
                      (assoc out k (system-deport component import))

                      (= type :expose)
                      (dissoc out k))))
            system
            (reverse order))))
