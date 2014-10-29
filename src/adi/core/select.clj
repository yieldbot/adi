(ns adi.core.select
  (:require [hara.common.checks :refer [hash-map? long?]]
            [hara.data
             [map :refer [assoc-nil]]
             [nested :refer [merge-nested]]]
            [adi.core.prepare :as prepare]
            [adi.process
             [normalise :as normalise]
             [pack :as pack]
             [emit :as emit]
             [unpack :as unpack]]
            [ribol.core :refer [raise]]
            [datomic.api :as datomic]))

(defn wrap-select-data [f]
  (fn [adi data]
    (cond (long? data)
          (if (or (-> adi :options :ban-ids)
                  (-> adi :options :ban-top-id))
            (raise [:id-banned {:data data}])
            #{data})

          (hash-map? data)
          (let [id  (or (get data :db/id)
                        (get-in data [:db :id]))
                qry (f adi data)]
            (if (-> adi :options :raw)
              #{qry}
              (let [res (->> (datomic/q qry (:db adi))
                             (map first)
                             (set))]
                (if id
                  (if (get res id) #{id} #{})
                  res)))))))

(defn wrap-select-return [f return]
  (fn [adi data]
    (let [res (f adi data)]
      (cond (-> adi :options :raw)
            res

            (-> adi :options :first)
            (if-let [fst (first res)]
              (return fst))

            :else
            (set (map return res))))))

(defn wrap-select-keyword [f]
  (fn [adi data]
    (if (keyword? data)
      (if (-> adi :options :ban-underscores)
        (raise [:keyword-banned {:data data}])
        (f adi {data '_}))
      (f adi data))))

(defn wrap-select-set [f]
  (fn [adi data]
    (if (set? data)
      (set (mapcat #(f adi %) data))
      (f adi data))))

(defn gen-query [adi data]
  (let [nadi (-> adi
                 (assoc-in [:process :input] data)
                 (update-in [:options]
                            dissoc
                            :schema-required
                            :schema-restrict
                            :schema-defaults)
                 (assoc :type "query")
                 (normalise/normalise)
                 (pack/pack)
                 (emit/emit))]
    ;;(println (-> nadi :process))
    (get-in nadi [:process :emitted])))

(defn select [adi data opts]
  (let [adi (prepare/prepare adi opts)
        return  (cond (-> adi :options :return-ids) identity
                      (-> adi :options :return-entities)
                      #(datomic/entity (:db adi) %)
                      :else #(unpack/unpack (datomic/entity (:db adi) %) adi))]
    ((-> gen-query
         (wrap-select-data)
         (wrap-select-keyword)
         (wrap-select-set)
         (wrap-select-return return))
     (assoc-nil adi :op :select) data)))
