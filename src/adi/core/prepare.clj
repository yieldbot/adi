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
  (cond (and (:allow model) (:return model))
        model

        (and (not (:allow model)) (:return model))
        (assoc model :allow (model/model-input access tsch))

        :else
        (let [imodel (model/model-input access tsch)
              rmodel (model/model-unpack imodel tsch)]
          (assoc-nil model :allow imodel :return rmodel))))

(defn model-return [model return tsch]
  (assoc model :return
         (-> return
             (model/model-input tsch)
             (model/model-unpack tsch))))

(defn prepare-model [adi]
  (let [op    (:op adi)
        model (or (:model adi) (if op (-> adi :profile op)))
        model  (if-let [access (:access adi)]
                 (model-access model access (-> adi :schema :tree))
                 model)
        model  (if-let [return (:return adi)]
                 (model-return model return (-> adi :schema :tree))
                 model)]
    (assoc adi :model model)))

(defn prepare [adi opts]
  (-> adi
      (merge-nested opts)
      (prepare-db)
      (prepare-model)))
