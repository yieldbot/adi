(ns adi.emit.characterise
  (:use hara.common
        [hara.control :only [if-let]]
        [adi.utils :only [?gensym iid]])
  (:refer-clojure :exclude [if-let]))

(declare characterise
         characterise-nout)

(defn characterise-gen-id [output env]
  (if-let [no-id? (nil? (get-in output [:db :id]))
           gen    (-> env :generate :ids)]
    (let [idgen (or (:function gen) iid)]
      (assoc-in output [:db :id] (idgen)))
    output))

(defn characterise-gen-sym [output env]
  (if-let [no-sym? (nil? (get-in output [:# :sym]))
           gen     (-> env :generate :syms)]
    (let [symgen (or (:function gen) ?gensym)]
      (assoc-in output [:# :sym] (symgen)))
    output))

(defn characterise
  ([pdata env]
     (characterise pdata env
                   (-> (characterise-gen-id {} env)
                       (characterise-gen-sym env))))
  ([pdata env output]
     (if-let [[k v] (first pdata)]
       (let []
         (recur (next pdata) env
                       (characterise-nout k v env output)))
       output)))

(defn characterise-nout [k v env output]
  (if-let [[meta] (-> env :schema :fgeni k)
        t (:type meta)]
    (cond
          (and (set? v) (= :ref t))
          (assoc-in output
                    [:refs-many (-> meta :ref :key)]
                    (set (map #(characterise % env) v)))

          (set? v)
          (assoc-in output [:data-many k] v)

          (= :ref t)
          (assoc-in output
                    [:refs-one (-> meta :ref :key)]
                    (characterise v env))

          :else
          (assoc-in output [:data-one k] v))

    (cond (= k :db)
          (assoc output k v)

          (= k :#)
          (assoc output k (merge-nested (output k) v))

          :else
          (error "key " k " not found in schema."))))
