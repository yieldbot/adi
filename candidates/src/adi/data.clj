(ns adi.data
  (:require [adi.data.normalise :refer [normalise]]
            [adi.data.pack.analyse :refer [analyse]]
            [adi.data.pack.review :refer [review]]
            [adi.data.unpack :refer [unpack]]
            [adi.data.characterise :refer [characterise]]
            [adi.data.emit.datoms :refer [datoms]]
            [adi.data.emit.query :refer [query]]))

(defn normalise* [data env]
  (if (-> env :options :skip-normalise) data
    (normalise data env)))

(defn pack* [ndata env]
  (if (-> env :options :skip-pack) ndata
    (let [pdata (-> ndata
                   (analyse env)
                   (review env))]
      pdata)))

(defn characterise* [pdata env]
  (characterise pdata env))

(defn unpack* [ent env]
  (unpack ent env))

(defn datoms* [pdata env]
  (datoms pdata))

(defn query* [pdata env]
  (query pdata env))
