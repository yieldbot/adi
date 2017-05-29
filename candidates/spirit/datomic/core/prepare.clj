(ns spirit.core.prepare
  (:require [hara.common.checks :refer [boolean?]]
            [hara.data
             [map :refer [assoc-nil]]
             [nested :refer [merge-nested]]]
            [datomic.api :as datomic]
            [spirit.core.model :as model]))

(defn prepare-db [spirit]
  (let [db (or (:db spirit) (datomic/db (:connection spirit)))
        db (if-let [t (:at spirit)] (datomic/as-of db t) db)]
    (assoc spirit :db db)))

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

(defn prepare-model [spirit]
  (let [op    (:op spirit)
        dft   (if (-> spirit :options :blank)
                :unchecked 
                :checked)
        model (or (:pipeline spirit) (if op (-> spirit :profile op)))
        model  (if-let [access (:access spirit)]
                 (model-access model access dft (-> spirit :schema :tree))
                 model)
        model  (if-let [pull (:pull spirit)]
                 (model-pull model pull dft (-> spirit :schema :tree))
                 model)]
    (assoc spirit :pipeline model)))

(defn prepare [spirit opts input]
  (-> spirit
      (merge-nested opts)
      (prepare-db)
      (prepare-model)
      (assoc-in [:process :input] input)
      (assoc-in [:tempids] (atom #{}))))
