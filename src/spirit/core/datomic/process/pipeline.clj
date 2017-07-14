(ns spirit.core.datomic.process.pipeline
  (:require [spirit.pipeline :as pipeline]
            [hara.data.path :as data]
            [hara.common.error :refer [error]]
            [hara.function.args :refer [op]]
            [spirit.core.datomic.process.pipeline
             [db :as db]
             [expression :as expression]
             [id :as id]
             [list :as list]
             [set :as set]
             [symbol :as symbol]
             [underscore :as underscore]
             [vector :as vector]]))

(def datomic-wrapper-fns
  {:underscore        underscore/wrap-branch-underscore
   :set               set/wrap-attr-set
   :vector-attr       vector/wrap-attr-vector
   :expression        expression/wrap-single-model-expression
   :db                db/wrap-db
   :id                id/wrap-single-id
   :list              list/wrap-single-list 
   :symbol            symbol/wrap-single-symbol 
   :vector-single     vector/wrap-single-vector})

(defn datomic-additions [{:keys [pipeline] :as datasource}]
  {:normalise              {:pre [:db]}
   :normalise-branch       {:post [:underscore]}
   :normalise-attr         {:pre [:set
                                  :vector-attr]}
   :normalise-expression   {:post [(if (:expression pipeline) :expression)]}
   :normalise-single       {:pre  [:id]
                            :post [:list
                                   :symbol
                                   :vector-single]}})

(defn normalise [{:keys [pipeline] :as datasource}]
  (let [data (-> datasource :process :input)
        ndata (if (-> datasource :options :skip-normalise)
                data
                (->> (merge datomic-wrapper-fns pipeline/normalise-wrapper-fns)
                     (pipeline/normalise-wrappers datasource (datomic-additions datasource))
                     (pipeline/normalise data datasource)))]
    (assoc-in datasource [:process :normalised] ndata)))
