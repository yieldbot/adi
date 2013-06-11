(ns adi.emit.adjust
  (:use [hara.common :only [error suppress long?]]
        [hara.hash-map :only [keyword-ns? keyword-stem]]
        [hara.control :only [if-let]])
  (:require [adi.schema :as as]
            [adi.emit.coerce :as ac])
  (:refer-clojure :exclude [if-let]))

;; ## Adjust Functions

(declare adjust
         adjust-chk-type adjust-chk-restrict
         adjust-value adjust-value-sets-only adjust-value-normal)

(defn adjust-patch-json [v meta env]
  (if-let [c (-> env :options :coerce?)]
    (let  [t     (:type meta)
           chk   (as/geni-type-checks (:type meta))]
      (cond (vector? v)
            (set (map #(adjust-patch-json % meta env) v))

            (and (-> env :options :query?) (= v "_")) '_

            :else
            (cond (suppress (chk v)) v

                  (string? c) (ac/coerce c v t)

                  :else v)))
    v))

(defn adjust
  "Adjusts the `v` according to `:cardinality` in `meta` or the `:sets-only`
   flag in `(env :options)`. Checks to see if the value is of correct type
   and has an optional `:restrict` parameter and it matches the `:restrict?` flag,
   also defined in `(env :options)`."
  [v meta env]
  (-> v
      (adjust-patch-json meta env)
      (adjust-chk-type meta env)
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
           chk (or (:restrict meta) (-> meta :enum :values))
           [cdoc chk] (if (vector? chk) chk [(str chk) chk])]
    (let [err-one  (format "The value %s does not meet the restriction: %s" v cdoc)
          err-many (format "The value/s [%s] do not meet the restriction: %s" v cdoc)]
      (adjust-value v meta chk env err-one err-many))
    v))

(defn adjust-value [v meta chk env err-one err-many]
  (if (-> env :options :sets-only?)
      (adjust-value-sets-only v meta chk env err-many)
      (adjust-value-normal v meta chk env err-one err-many)))

(defn adjust-patch-enum [v meta]
  (if (keyword-ns? v (-> meta :enum :ns))
    (keyword-stem v)
    v))

(defn adjust-safe-check [v meta chk env]
  (or  (and (-> env :options :query?)
            (or (= v '_)
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
