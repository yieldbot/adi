(ns adi.core.environment
  (:require [hara.common.checks :refer [boolean?]]
            [hara.data.map :refer [assoc-nil]]
            [datomic.api :as datomic]
            [adi.core.model :as model]))

(def option-keys #{:ban-expressions :ban-ids :ban-top-id
                   :schema-required :schema-restrict :schema-defaults
                   :model-typecheck :model-coerce :skip-normalise
                   :skip-typecheck :first :ids
                   :generate-ids :generate-syms
                   :raw :simulate})

(defn- auto-pair-seq
  ([arr] (auto-pair-seq [] arr))
  ([output [x y & xs :as arr]]
     (cond (nil? x) output
           (or (and (option-keys x) (not (boolean? y)))
               (and (nil? y) (nil? xs)))
           (recur (conj output [x true]) (next arr))

           (and (option-keys x) (boolean? y))
           (recur (conj output [x y]) xs)

           :else (recur (conj output [x y]) xs))))

(defn- wrap-env-setup-db [f]
  (fn [adi data]
    (let [db (or (:db adi) (datomic/db (:conn adi)))
          db (if-let [t (:at adi)] (datomic/as-of db t) db)]
      (f (assoc adi :db db) data))))

(defn- add-model-access [model access tsch]
  (cond (and (:allow model) (:return model))
        model

        (and (not (:allow model)) (:return model))
        (assoc model :allow (model/model-input access tsch))

        :else
        (let [imodel (model/model-input access tsch)
              rmodel (model/model-unpack imodel tsch)]
          (assoc-nil model :allow imodel :return rmodel))))

(defn- add-model-return [model return tsch]
  (assoc model :return
    (-> return
        (model/model-input tsch)
        (model/model-unpack tsch))))

(defn- wrap-env-setup-model [f options]
  (fn [adi data]
    (let [op  (:op adi)
          model (or (:model adi) (if op (-> adi :profile op)))
          options (merge (:options adi) (:options model) options)
          model  (if-let [access (:access adi)]
                   (add-model-access model access (-> adi :schema :tree))
                   model)
          model  (if-let [return (:return adi)]
                   (add-model-return model return (-> adi :schema :tree))
                   model)]
      (f (assoc adi :model model) data))))

(defn- wrap-env-setup [f args]
  (fn [adi data]
    (let [pargs (into {} (auto-pair-seq args))
          options (select-keys pargs option-keys)
          env-args (apply dissoc pargs option-keys)
          f  (wrap-env-setup-db f)
          f  (wrap-env-setup-model f options)]
      (f (-> adi
             (assoc :data data)
             (merge env-args)
             (update-in [:options] merge options) )
         data))))

(defn setup [adi args]
  ((wrap-env-setup (fn [adi data] adi) args) adi nil))
