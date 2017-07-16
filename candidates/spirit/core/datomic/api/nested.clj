(ns spirit.core.datomic.api.nested
  (:require [hara.string.path :as path]
            [hara.common.checks :refer [long?]]
            [clojure.walk :as walk]
            [spirit.core.datomic.api
             [prepare :as prepare]
             [retract :as retract]
             [select :as select]
             [transaction :as transaction]]
            [hara.event :refer [raise]]))

(defn search-path-analysis
  ([spath tsch]
     (search-path-analysis spath tsch nil []))
  ([spath tsch last-res all-res]
     (if-let [[k v] (first spath)]
       (let [ks (path/split k)
             ks (if-let [nss (:ns last-res)]
                  (cons nss ks) ks)
             [attr] (get-in tsch ks)
             res  (-> (:ref attr)
                      (select-keys  [:val :rval :ns])
                      (assoc :term v))]
         (recur (next spath) tsch res (conj all-res res)))
       all-res)))

(defn build-search-term [vterm all-res]
  (let [[res & next-res] all-res]
    (if-not res
      vterm
      (let [_    (if-not (:ns res)
                   (raise [:cannot-perform-reverse-lookup {:value res}]))
            mterm (:term res)
            mterm (cond (long? mterm)
                        (if (empty? next-res) {:db/id mterm} {:+/db/id mterm})

                        (= '_ mterm) nil

                        :else mterm
                   )
            nterm (merge (assoc {} (:rval res) vterm) mterm)
            nterm (if (empty? next-res)
                    {(:ns res) nterm}
                    nterm)]
        (recur nterm next-res)))))

(defn build-search-term-fn [all-res]
  (let [id (gensym)
        data (build-search-term id all-res)
        data (walk/postwalk (fn [x] (if (or (= x '_) (list? x))
                                   (list 'quote x)
                                   x))
                       data)]
    (eval (list 'fn [id] data))))

(defn update-in! [datasource data path update opts]
  (assert (even? (count path)) "The path must have a even number of items.")
  (let [datasource (prepare/prepare datasource opts data)
        ids (select/select datasource data {:options {:raw false}
                                     :return :ids})
        spath (partition 2 path)
        svec  (search-path-analysis spath (-> datasource :schema :tree))
        ndata-fn (build-search-term-fn svec)
        last-ns (:ns (last svec))
        update (if last-ns {last-ns update} update)
        output  (mapcat
                 #(transaction/update! (dissoc datasource :pipeline) (ndata-fn %) update
                                       {:options {:raw true
                                                  :ban-body-ids false
                                                  :ban-ids false
                                                  :ban-top-id false}}) ids)
        sids (map :db/id output)
        transact (-> datasource
                     (assoc :transact :datomic)
                     (assoc-in [:process :emitted] output)
                     (transaction/transact-fn))]
    (if (or (-> datasource :transact (= :datomic))
            (-> datasource :options :raw))
      transact
      (select/select (assoc datasource :db (:db-after transact))
                     (set sids)
                     (merge opts {:options {:debug false
                                            :ban-ids false
                                            :ban-top-id false
                                            :ids true}})))))

(defn delete-in! [datasource data path opts]
  (assert (even? (count path)) "The path must have a even number of items.")
  (let [datasource (prepare/prepare datasource opts data)
        ids (select/select datasource data {:options {:raw false}
                                     :return :ids} )
        spath (partition 2 path)
        svec  (search-path-analysis spath (-> datasource :schema :tree))
        ndata-fn (build-search-term-fn svec)
        output  (mapcat
                 #(transaction/delete! (dissoc datasource :pipeline) (ndata-fn %)
                                       {:options {:raw true
                                                  :ban-body-ids false
                                                  :ban-ids false
                                                  :ban-top-id false}}) ids)
        sids (map second output)
        transact (-> datasource
                     (assoc :transact :datomic)
                     (assoc-in [:process :emitted] output)
                     (transaction/transact-fn))]
    (if (or (-> datasource :transact (= :datomic))
            (-> datasource :options :raw))
      transact
      (select/select (assoc datasource :db (:db-before transact))
                     (set sids)
                     (merge opts {:options {:debug false
                                            :ban-ids false
                                            :ban-top-id false
                                            :ids true}})))))


(defn add-ns-entry [ns entry]
  (cond (vector? entry)
        [(path/join [ns (first entry)]) (second entry)]
        (keyword? entry) (path/join [ns entry])))

(defn retract-in! [datasource data path retracts opts]
  (assert (even? (count path)) "The path must have a even number of items.")
  (let [datasource (prepare/prepare datasource opts data)
        ids (select/select datasource data {:options {:first false
                                               :raw false}
                                     :return :ids})
        spath (partition 2 path)
        svec  (search-path-analysis spath (-> datasource :schema :tree))
        ndata-fn (build-search-term-fn svec)
        last-ns (:ns (last svec))
        nretracts (set (map #(add-ns-entry last-ns %) retracts))

        output  (mapcat
                 (fn [id] (retract/retract! (dissoc datasource :pipeline)
                                           (ndata-fn id)
                                           nretracts
                                           {:options {:raw true
                                                      :ban-body-ids false
                                                      :ban-ids false
                                                      :ban-top-id false}})) ids)
        sids (map second output)
        transact (-> datasource
                     (assoc :transact :datomic)
                     (assoc-in [:process :emitted] output)
                     (transaction/transact-fn))]
    (if (or (-> datasource :options :raw)
            (-> datasource :transact (= :datomic)))
      transact
      (select/select (assoc datasource :db (:db-after transact))
                     (set sids)
                     (merge opts {:options {:debug false
                                            :ban-ids false
                                            :ban-top-id false
                                            :ids true}})))))
