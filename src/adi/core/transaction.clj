(ns adi.core.transaction
  (:require [hara.common.checks :refer [hash-map? long?]]
            [hara.data.nested :refer [merge-nil-nested]]
            [adi.data.common :refer [iid-seed]]
            [adi.core
             [prepare :as prepare]
             [select :as select]
             [model :as model]
             [link :as link]]
            [adi.process
             [normalise :as normalise]
             [pack :as pack]
             [emit :as emit]
             [unpack :as unpack]]
            [ribol.core :refer [raise]]
            [datomic.api :as datomic]))

(defn gen-datoms [adi data]
  (let [nadi (-> adi
                 (assoc :type "datoms")
                 (assoc-in [:process :input] data)
                 (normalise/normalise)
                 (pack/pack)
                 (emit/emit))]
    (clojure.pprint/pprint (-> nadi :process :analysed))
    (println (-> nadi :process :emitted))
    (get-in nadi [:process :emitted])))

(defn wrap-transaction-return [f & [ids]]
  (fn [adi data]
    (let [trs  (:transact adi)
          dtms (f adi data)]
      (cond (-> adi :options :raw)
            dtms

            (-> adi :options :simulate)
            (assoc adi :db (:db-after (datomic/with (:db adi) dtms)))

            :else
            (condp = trs
              ;; @(datomic/transact (:connection adi) dtms)
              nil      (let [res @(datomic/transact (:connection adi) dtms)]
                         (println (map #(vector (iid-seed %)
                                                (datomic/resolve-tempid (datomic/db (:connection adi))
                                                                        (:tempids res)
                                                                        %))
                                       (->> dtms
                                            (filter hash-map?)
                                            (map :db/id)
                                            (set))))
                         res)

              :promise (datomic/transact (:connection adi) dtms)
              :async   (datomic/transact-async (:connection adi) dtms)
              :full    (let [res @(datomic/transact (:connection adi) dtms)
                             ndb (:db-after res)]
                         (if ids
                           (select/select adi ids {:db ndb
                                                   :options {:return-ids false
                                                             :return-entities false}})
                           res))
              :compare (let [res @(datomic/transact (:connection adi) dtms)
                             ndb (:db-after res)]
                         (if ids
                           [(select/select adi ids {:options {:return-ids false
                                                              :return-entities false}})
                            (select/select adi ids {:db ndb
                                                    :options {:return-ids false
                                                              :return-entities false}})]
                           res)))))))

(defn wrap-vector-inserts [f]
  (fn [adi data]
    (if (vector? data)
      (let [vfn (fn [adi data]
                  (mapcat #(f adi %) data))]
        (vfn adi data))
      (f adi data))))

(defn insert! [adi data opts]
  (let [opts (merge-nil-nested opts {:options {:schema-restrict true
                                               :schema-required true
                                               :schema-defaults true}})
        adi (-> adi
                (prepare/prepare opts)
                (assoc :op :insert))
        f   (-> gen-datoms
                (wrap-vector-inserts)
                (wrap-transaction-return))]
    (f adi data)))

(defn transact! [adi datoms opts]
  (let [adi (-> adi
                (prepare/prepare opts)
                (assoc :op :transact))
        f (-> (fn [_ datoms] datoms)
              (wrap-transaction-return))]
    (f adi datoms)))

(defn delete! [adi data opts]
  (let [adi (-> adi
                (prepare/prepare opts)
                (assoc :op :delete))
        ids (select/select adi data {:options {:raw false
                                               :return-ids true}})
        f (-> (fn [adi data]
                (map (fn [x] [:db.fn/retractEntity x]) ids))
              (wrap-transaction-return ids))]
    (f adi data)))

(defn update! [adi data update opts]
  (let [adi (prepare/prepare adi opts)
        ids (select/select adi data {:options {:raw false :return-ids true}})
        updates (mapv (fn [id] (assoc update :db/id id)) ids)
        adi (if (-> adi :options :ban-ids)
              (-> adi
                  (assoc-in [:options :ban-ids] false)
                  (assoc-in [:options :ban-body-ids] true))
              adi)
        adi (assoc adi :op :modify)
        f  (-> gen-datoms
               (wrap-vector-inserts)
               (wrap-transaction-return ids))]
    (f adi updates)))

(defn delete-all! [adi data opts]
  (let [adi (prepare/prepare adi opts)
        ids (select/select adi data {:options {:raw false :return-ids true}})
        rmodel (if-let [imodel (-> adi :model :allow)]
                 (model/model-unpack imodel (-> adi :schema :tree))
                 (raise :missing-allow-model))
        ents    (map #(datomic/entity (:db adi) %) ids)
        all-ids (mapcat (fn [ent]
                          (link/linked-ids ent rmodel (-> adi :schema :flat)))
                        ents)
        output  (delete! adi (set all-ids) {:options {:raw true
                                                      :ban-ids false
                                                      :ban-top-id false}})
        f (-> (fn [adi data] output)
              (wrap-transaction-return ids))]
    (f adi output)))
