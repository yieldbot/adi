(ns adi.data
  (:use [datomic.api :only [tempid]]
        [adi.schema :as as]
        adi.utils))

(defn iid
  "Constructs a new id"
  ([] (tempid :db.part/user))
  ([obj]
     (let [v (hash obj)
           v (if (< 0 v) (- v) v)]
       (tempid :db.part/user v ))))

(defn correct-type? [meta v]
  "Checks to see if v matches the description in the meta.
   throws an exception if the v does not"
  (let [t (:type meta)
        c (or (:cardinality meta) :one)
        chk (as/type-checks t)]
    (cond (and (= c :one) (not (chk v)))
          (throw (Exception. (format "The value %s is not of type %s" v t)))
          (and (= c :many) (not (set? v)))
          (throw (Exception. (format "%s needs to be a set" v)))
          (and (= c :many) (not (every? chk v)))
          (throw (Exception. (format "Not every value in %s is not of type %s" v t))))
    true))

(def k-ns-global :+)
(def k-ns-fn :#)

(defn find-nskv [k data]
  (let [nskv (k-unmerge k)
        gnskv (cons :+ nskv)
        trials (lazy-seq [[:+ k] gnskv [k] nskv [(k-merge gnskv)]])]
    (->> trials
         (filter #(get-in data %))
         first)))

(defn find-db-id [val]
  (cond (integer? val) val

         (ref? val)
         (if-let [nskv (find-nskv :db/id val)]
           (get-in val nskv))

         :else (throw (Exception. (str "Not an integer, entity nor hashmap")))))


(declare process
         process-id process-fns process-assoc process-ref process-default)

(defn process
  "Processes the data according to the schema specified to form a tree-like
   structure of refs and values for the next step of characterisation."
  ([fsm data] (process fsm data true))
  ([fsm data defaults?] (process fsm fsm data {} defaults?))
  ([fsm nfsm data output defaults?]
     (if-let [[k [meta]] (first nfsm)]
       (let [tk     (find-nskv k data)
             v      (cond tk (get-in data tk)
                          defaults? (process-default meta k data))
             output (if v
                      (process-assoc fsm meta output k v defaults?)
                      output)]
         (process fsm (rest nfsm) data output defaults?))
       (->> output
            (process-id data)
            (process-fns data)))))

(defn- process-id [data output]
  (if-let [ks (find-nskv :db/id data)]
    (assoc output :db/id (get-in data ks))
    output))

(defn- process-fns [data output]
  (let [fn-m (select-keys (treeify-keys data) [:#])]
    (merge output (flatten-once-keys fn-m))))

(defn- process-default [meta k data]
  (let [n  (k-unmerge (k-ns k))
        m (treeify-keys data)]
    (if (get-in m n) (:default meta))))

(defn- process-ref [fsm meta v defaults?]
  (let [nsv   (k-unmerge (:ref-ns meta))

        refv (extend-keys (treeify-keys v) nsv [:+])]
    (process fsm refv defaults?)))

(defn- process-assoc [fsm meta output k v defaults?]
  (if (correct-type? meta v)
    (let [t (:type meta)
          c (or (:cardinality meta) :one)]
      (cond (not= t :ref)
            (assoc output k v)

            (= c :one)
            (assoc output k (process-ref fsm meta v defaults?))

            (= c :many)
            (assoc output k (set (map #(process-ref fsm meta % defaults?) v)))))))
