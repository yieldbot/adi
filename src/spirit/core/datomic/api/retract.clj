(ns spirit.core.datomic.api.retract
  (:require [hara.common.checks :refer [hash-map?]]
            [hara.string.path :as path]
            [spirit.core.datomic.data.checks :refer [entity?]]
            [spirit.core.datomic.api
             [select :as select]
             [prepare :as prepare]
             [transaction :as transaction]]
            [datomic.api :as datomic]
            [hara.event :refer [raise]]))

(defn- walk-path-gather [et attr]
  (let [rf (get et (-> attr :ref :key))]
    (cond (set? rf) rf
          :else (list rf))))

(defn- walk-path
  ([ets path tsch] (walk-path ets [] path tsch tsch))
  ([ets ppath path psch tsch]
      (let [[k & more] path]
        (cond (nil? k) (raise :walk-path-invalid-key)

              (= k :+) (recur ets [] more tsch tsch)

              (and (= k :db) (= :id (first more)))
              (raise :walk-path-db-id-not-allowed)

              :else
              (let [node (get psch k)]
                (cond (nil? node)
                      (raise [:walk-path-key-not-in-schema {:k k}])

                      (hash-map? node)
                      (recur ets (conj ppath k) more node tsch)

                      (vector? node)
                      (let [[{t :type :as attr}] node]
                        (cond (not= t :ref)
                              (if (empty? more) [ets (path/join (conj ppath k))]
                                  (raise :walk-path-cannot-walk-past-non-ref))

                              (= t :ref)
                              (if (empty? more) [ets (-> attr :ref :key)]
                                  (let [n-ets   (mapcat #(walk-path-gather % attr) ets)
                                        nss (-> attr :ref :ns)
                                        n-ppath (if nss [nss] [])
                                        n-psch  (if nss (get tsch nss) tsch)]
                                    (recur n-ets n-ppath more n-psch tsch)))))))))))

(defn- wrap-retract-val-set [f]
  (fn [et path retract]
    (let [rf (get et path)]
      (cond (set? rf) (map #(f % retract) rf)
            :else [(f rf retract)]))))

(defn- retract-val [v retract]
  (cond (entity? v)
        (let [id (:db/id v)]
          (if (retract id) id))

        :else (if (retract v) v)))

(defn- make-entry-recs-single [et path retract]
  (let [vals (filter identity ((wrap-retract-val-set retract-val) et path retract))]
    (map (fn [v] [:db/retract (:db/id et) path v]) vals)))

(defn- make-entry-recs [ets entry datasource]
  (let [[path retract] (cond (keyword? entry) [entry (constantly true)]
                             (vector? entry) [(first entry) (second entry)]
                             :else (raise [:invalid-retraction-entry {:value entry}]))
        spath         (path/split path)
        _             (if-let [mod (-> datasource :pipeline :allow)]
                        (if-not (get-in mod spath)
                          (raise [:pipeline-path-access-not-allowed {:path spath}])))
        [ets path]    (walk-path ets spath (-> datasource :schema :tree))]
    (mapcat #(make-entry-recs-single % path retract) ets)))

(defn retract! [datasource data retracts opts]
  (let [datasource (prepare/prepare datasource opts data)
        ids (select/select datasource data {:options {:raw false
                                               :first false}
                                     :return :ids})
        ets (map #(datomic/entity (:db datasource) %) ids)
        datoms (mapcat #(make-entry-recs ets % datasource) retracts)
        transact-fn   (-> transaction/transact-base
                          (transaction/wrap-transact-options)
                          (transaction/wrap-delete-results :db-after)
                          (select/wrap-pull-raw))]
    (-> datasource
        (prepare/prepare opts datoms)
        (transaction/prepare-tempids)
        (assoc :op :retract)
        transact-fn)))
