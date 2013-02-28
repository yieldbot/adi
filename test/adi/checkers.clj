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