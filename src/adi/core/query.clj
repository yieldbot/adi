(ns adi.core.query
  (:require [datomic.api :as datomic]
            [adi.core [prepare :as prepare]]
            [adi.process
             [unpack :as unpack]]))

(defn- wrap-query-return [f ret-fn]
  (fn [adi data qargs]
    (cond (-> adi :options :raw)
          data

          :else
          (let [res (f adi data qargs)]
            (cond
             (-> adi :options :first)
             (if-let [fst (first res)]
               (ret-fn fst))

             :else
             (set (map ret-fn res)))))))

(defn query [adi data qargs opts]
  (let [adi (prepare/prepare adi opts data)
       return  (cond (-> adi :options :return-datomic) identity
                     (-> adi :options :return-ids) first
                     (-> adi :options :return-entities)
                     #(datomic/entity (:db adi) (first %))
                     :else #(unpack/unpack (datomic/entity (:db adi) (first %)) adi))
       f  (-> (fn [adi data qargs] (apply datomic/q data (:db adi) qargs))
              (wrap-query-return return))]
   (f (assoc adi :op :query) data qargs)))
