(ns spirit.core.datomic.api.prepare
  (:require [hara.common.checks :refer [boolean?]]
            [hara.data
             [map :refer [assoc-nil]]
             [nested :refer [merge-nested]]]
            [datomic.api :as datomic]
            [spirit.core.datomic.api.model :as model]))

(defn prepare-db [datasource]
  (let [db (or (:db datasource) (datomic/db (:connection datasource)))
        db (if-let [t (:at datasource)] (datomic/as-of db t) db)]
    (assoc datasource :db db)))

(defn model-access [model access dft tsch]
  (cond (and (:allow model) (:pull model))
        model

        (and (not (:allow model)) (:pull model))
        (assoc model :allow (model/model-input access dft tsch))

        :else
        (let [imodel (model/model-input access dft tsch)
              rmodel (model/model-unpack imodel tsch)]
          (assoc-nil model :allow imodel :pull rmodel))))

(defn model-pull [model pull dft tsch]
  (assoc model :pull
         (-> pull
             (model/model-input dft tsch)
             (model/model-unpack tsch))))

(defn prepare-model [datasource]
  (let [op    (:op datasource)
        dft   (if (-> datasource :options :blank)
                :unchecked 
                :checked)
        model (or (:pipeline datasource) (if op (-> datasource :profile op)))
        model  (if-let [access (:access datasource)]
                 (model-access model access dft (-> datasource :schema :tree))
                 model)
        model  (if-let [pull (:pull datasource)]
                 (model-pull model pull dft (-> datasource :schema :tree))
                 model)]
    (assoc datasource :pipeline model)))

(defn prepare [datasource opts input]
  (-> datasource
      (merge-nested opts)
      (prepare-db)
      (prepare-model)
      (assoc-in [:process :input] input)
      (assoc-in [:tempids] (atom #{}))))
