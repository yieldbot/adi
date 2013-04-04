(ns adi.checkers
  (:use adi.utils))

(defn has-length [l]
  (fn [x]
    (and (or (hash-set? x) (seq? x))
         (= l (count x)))))

(defn has-data [k v]
  (fn [x]
    (cond (hash-set? v)
          (contains? v (k x))
          :else (= v (k x)))))

(defn results-contain
  ([k v]
     (let [chk (has-data k v)]
       (fn [x] (every? chk x))))
  ([m]
     (fn [x]
       (every? #((apply results-contain %) x) m))))

(defn exclude-id [m]
  (fn [val]
    (= (dissoc val :db/id) m)))

(defn exclude-ids [ms]
  (fn [val]
    (= (map #(dissoc % :db/id) val) ms)))

(defn nest-in-fn [sym c]
  (cond (vector? c)
        (list sym (mapv #(nest-in-fn sym %) c))

        (hash-map? c)
        (->> c
             (map (fn [[k v]]
                    [k (nest-in-fn sym v)]))
             (into {})
             (list sym))
        (hash-set? c)
        (list sym (set (map #(nest-in-fn sym %) c)))

        :else c))

(defmacro just-in [c & args]
  (concat (nest-in-fn 'just c)
          args))

(defmacro contains-in [c & args]
  (concat (nest-in-fn 'contains c)
          args))
