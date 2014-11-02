(ns adi.core.prepare
  (:require [hara.common.checks :refer [boolean?]]
            [hara.data
             [map :refer [assoc-nil]]
             [nested :refer [merge-nested]]]
            [datomic.api :as datomic]
            [adi.core.model :as model]))

(defn prepare-db [adi]
  (let [db (or (:db adi) (datomic/db (:connection adi)))
        db (if-let [t (:at adi)] (datomic/as-of db t) db)]
    (assoc adi :db db)))

(defn model-access [model access tsch]
  (cond (and (:allow model) (:pull model))
        model

        (and (not (:allow model)) (:pull model))
        (assoc model :allow (model/model-input access tsch))

        :else
        (let [imodel (model/model-input access tsch)
              rmodel (model/model-unpack imodel tsch)]
          (assoc-nil model :allow imodel :pull rmodel))))

(defn model-pull [model pull tsch]
  (assoc model :pull
         (-> pull
             (model/model-input tsch)
             (model/model-unpack tsch))))

(defn prepare-model [adi]
  (let [op    (:op adi)
        model (or (:model adi) (if op (-> adi :profile op)))
        model  (if-let [access (:access adi)]
                 (model-access model access (-> adi :schema :tree))
                 model)
        model  (if-let [pull (:pull adi)]
                 (model-pull model pull (-> adi :schema :tree))
                 model)]
    (assoc adi :model model)))

(defn prepare [adi opts input]
  (-> adi
      (merge-nested opts)
      (prepare-db)
      (prepare-model)
      (assoc-in [:process :input] input)
      (assoc-in [:tempids] (atom #{}))))
