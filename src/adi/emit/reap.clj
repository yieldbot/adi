(ns adi.emit.reap
  (:use hara.common
        hara.hash-map
        [hara.control :only [if-let]])
  (:require [adi.schema :as as])
  (:refer-clojure :exclude [if-let]))


(declare reap
         reap-entity)

#_(defn reap-init-create-keyword-hashmap
  ([nsm env]
     (let [fields (vals nsm)]
       (->> fields
            (filter #(not= :reverse (-> % :ref :type)))
            (map (fn [x] [(:ident x) (if (= :ref (:type x)) :follow :show)]))
            (into {})))))

#_(defn reap-init-create-ns-view
  ([vw env] (reap-init-create-ns-view {} (seq vw) env))
  ([output [k & ks] env]
     (if (nil? k) output
         (let [nsm (-> env :schema :geni (get-in (keyword-split k)))]
           (cond (hash-map? nsm)
               (recur (merge output (reap-init-create-ns-hashmap nsm env))
                      ks env)

               (vector? nsm)
               (recur (assoc output k :show) ks env)

               :else (recur output ks env))))))

(defn reap-init-create-keyword-hashmap
  ([output [fd & fds] env]
     (cond (nil? fd) output
           (vector? fd)
           (let [sch (first fd)]
             (if (not= :reverse (-> sch :ref :type))
               (recur (assoc output
                        (:ident sch)
                        :show
                        ;;(if (= :ref (:type sch)) :follow :show)
                        )
                      fds env)
               (recur output fds env)))

           (hash-map? fd)
           (recur (merge output
                         (reap-init-create-keyword-hashmap fd env))
                  fds env)))
  ([nsm env]
     (let [fields (vals nsm)]
       (reap-init-create-keyword-hashmap {} fields env))))

(defn reap-init-create-keyword-view
  [k env]
  (let [nsm (-> env :schema :geni (get-in (keyword-split k)))]
    (cond (hash-map? nsm)
          (reap-init-create-keyword-hashmap nsm env)
          (vector? nsm) {k :show}
          :else {})))


(defn reap-init-create-field-keyword
  [output k env]
  (if-let [field (-> env :schema :geni (get-in (keyword-split k)) first)]
    (case (:type field)
      :ref (assoc output k :follow)
      (assoc output k :show))))

(defn reap-init-create-field-view
  ([vw env] (reap-init-create-field-view {} (seq vw) env))
  ([output [k & ks] env]
     (if (nil? k) output
         (cond (keyword? k)
               (recur (reap-init-create-field-keyword output k env) ks env)

               (vector? k)
               (let [[k type] k]
                 (recur (assoc output k type) ks env))))))

(declare reap-init-create-view)

(defn reap-init-create-multi-view
  ([vw env] (reap-init-create-multi-view {} vw env))
  ([output [vw & vws] env]
     (if (nil? vw) output
         (recur (merge output (reap-init-create-view vw env))
                vws
                env))))

(defn reap-init-create-view [vw env]
  (cond (keyword? vw)
        (reap-init-create-keyword-view vw env)

        (hash-map? vw)
        (flatten-keys-nested vw)

        (hash-set? vw)
        (reap-init-create-field-view vw env)

        (vector? vw)
        (reap-init-create-multi-view vw env)

        :else {}))

(defn reap-init-env [env]
  (if-let [vw (-> env :view)]
    (assoc env :view
           (reap-init-create-view vw env))
    env))

(defn reap-ref
  [k rf meta env exclude]
  (let [vw  (or (-> env :view) {})
        dir (or (vw k)
                (-> env :reap :refs)
                :hide)
        id (:db/id rf)]
    (cond (= dir :hide) nil

          (= dir :show) (if id
                          {:+ {:db {:id id}}}
                          {})

          (exclude id) {:+ {:db {:id id}}}

          (= dir :follow)
          (let [rns (keyword-split (-> meta :ref :ns))
                nm  (reap rf env exclude)

                cm  (get-in nm rns)
                xm  (dissoc-in nm rns)]

            (if (empty? xm) (or cm {})
                (merge cm {:+ xm}))))))

(defn reap-assoc-ref
  [output k v meta env exclude]
  (cond (or (hash-set? v) (vector? v))
        (assoc output k
               (-> (map #(reap-ref k % meta env exclude) v)
                   (set)
                   (disj nil)))
        :else
        (if-let [rout (reap-ref k v meta env exclude)]
          (assoc output k rout)
          output)))

(defn reap-assoc-data
  [output k v env]

  (let [vw  (or (-> env :view) {})
        dir (or (vw k)
                (-> env :reap :data)
                :hide)]
    (cond (= dir :show)
          (assoc output k v)

          :else output)))

(defn reap-assoc
  ([output k v meta env exclude]
     (let [t   (:type meta)]
       (cond (not= t :ref)
             (reap-assoc-data output k v env)

             :else
             (reap-assoc-ref output k v meta env exclude)))))

(defn reap-entity
  ([entity env exclude]
     (reap-entity {} entity env exclude))
  ([output entity env exclude]
     (if-let [[k v] (first entity)]
       (if-let [[meta] (-> env :schema :fgeni k)]
         (-> output
             (reap-assoc k v meta env exclude)
             (recur (next entity) env exclude))
         (recur output (next entity) env exclude))
       output)))

(defn reap-entity-init [entity env exclude]
  (let [id (:db/id entity)
        exclude (if id (conj exclude id) exclude)
        output  (if (and id (not= :hide (-> env :reap :ids)))
                  {:db/id id} {})]
    [output exclude]))


(defn reap-reverse-refs
  ([entity env exclude vw]
     (reap-reverse-refs {} entity env exclude vw))
  ([output entity env exclude vw]
     ;;(println vw)
     (if-let [[k dir] (first vw)]
       (if-let [;;_  (do (println k dir) true)
                [meta] (-> env :schema :fgeni k)
                _ (= :reverse (-> meta :ref :type))
                v (get entity (-> meta :ref :key))]
         (-> output
             (reap-assoc k v meta env exclude)
             (recur entity env exclude (next vw)))
         (recur output entity env exclude (next vw)))
       output)))

(defn reap
  ([entity env]
     (reap entity env #{}))
  ([entity env exclude]
     (let [env     (reap-init-env env)
           [output exclude] (reap-entity-init entity env exclude)
           output  (reap-entity output entity env exclude)
           output  (reap-reverse-refs output entity env exclude (:view env))]
       (treeify-keys output))))
