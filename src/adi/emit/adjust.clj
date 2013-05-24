(ns adi.emit.adjust
  (:use [hara.common :only [error suppress long?]]
        [hara.hash-map :only [keyword-ns? keyword-stem]]
        [hara.control :only [if-let]])
  (:require [adi.schema :as as])
  (:refer-clojure :exclude [if-let]))

;; ## Adjust Functions

(declare adjust
         adjust-chk-type adjust-chk-restrict
         adjust-value adjust-value-sets-only adjust-value-normal)

(defn adjust
  "Adjusts the `v` according to `:cardinality` in `meta` or the `:sets-only`
   flag in `(env :options)`. Checks to see if the value is of correct type
   and has an optional `:restrict` parameter and it matches the `:restrict?` flag,
   also defined in `(env :options)`."
  [v meta env]
  (-> (adjust-chk-type v meta env)
      (adjust-chk-restrict meta env)))

(defn adjust-chk-type [v meta env]
  (let [chk      (as/geni-type-checks (:type meta))
        err-one  (format "The value %s is not of type %s, it is of type %s"
                        v (:type meta) (type v))
        err-many (format "The value/s [%s] are not of type %s"
                         v meta)]
    (adjust-value v meta chk env err-one err-many)))

(defn adjust-chk-restrict [v meta env]
  (if-let [restrict? (-> env :options :restrict?)
           chk (or (:restrict meta) (-> meta :enum :values))]
    (let [err-one  (format "The value %s does not meet the restriction %s" v chk)
          err-many (format "The value/s [%s] do not meet the restriction %s" v chk)]
      (adjust-value v meta chk env err-one err-many))
    v))

#_(defn adjust-patch-enum [v meta]
  (if (and (= :enum (:type meta))
           (keyword-ns? v (-> meta :enum :ns)))
    (keyword-stem v)
    v))

(defn adjust-value [v meta chk env err-one err-many]
  (if (-> env :options :sets-only?)
      (adjust-value-sets-only v meta chk env err-many)
      (adjust-value-normal v meta chk env err-one err-many)))

#_(defn adjust-safe-check [v meta chk env]
  (or (suppress (chk (adjust-patch-enum v meta)))
      (and (-> env :options :query?)
           (or (= v '_)
               (vector? v) ;; TODO This is going out
               (list? v)))
      ))

(defn adjust-patch-enum [v meta]
  (if (keyword-ns? v (-> meta :enum :ns))
    (keyword-stem v)
    v))

(defn adjust-safe-check [v meta chk env]
  (or  (and (-> env :options :query?)
            (or (= v '_)
                (vector? v) ;; TODO This is going out
                (list? v)))
       (and (= :enum (:type meta))
            (if (long? v)
              v
              (suppress (chk (adjust-patch-enum v meta)))))

       (suppress (chk v))))




(defn adjust-value-sets-only [v meta chk env err-many]
  (cond (and (set? v) (every? #(adjust-safe-check % meta chk env) v)) v
        (adjust-safe-check v meta chk env) #{v}

        :else (error err-many)))

(defn adjust-value-normal [v meta chk env err-one err-many]
  (let [c (or (:cardinality meta) :one)]
    (cond (= c :one)
          (if (adjust-safe-check v meta chk env) v
              (error err-one))

          (= c :many)
          (cond (adjust-safe-check v meta chk env) #{v}
                (and (set? v) (every? #(adjust-safe-check % meta chk env) v)) v
                :else (error err-many)))))
