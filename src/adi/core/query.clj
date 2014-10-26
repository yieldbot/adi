(ns adi.core.query
  (:require [datomic.api :as datomic]
            [adi.core [prepare :as prepare]]
            [adi.process
             [unpack :as unpack]]))

(defn- wrap-query-return [f ret-fn]
  (fn [adi data]
    (cond (-> adi :options :raw)
          data

          :else
          (let [res (f adi data)]
            (cond
             (-> adi :options :first)
             (if-let [fst (first res)]
               (ret-fn fst))

             :else
             (set (map ret-fn res)))))))

(defn query [adi data args opts]
  (let [adi (prepare/prepare adi opts)
       return  (cond (-> adi :options :return-datomic) identity
                     (-> adi :options :return-ids) first
                     (-> adi :options :return-entities)
                     #(datomic/entity (:db adi) (first %))
                     :else #(unpack/unpack (datomic/entity (:db adi) (first %)) adi))
       f  (-> (fn [adi data] (apply datomic/q data (:db adi) args))
              (wrap-query-return return))]
   (f (assoc adi :op :query) data)))
