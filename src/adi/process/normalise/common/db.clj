(ns adi.process.normalise.common.db)

(defn db-id-syms
  "creates a compatible db/id symbol
  (db-id-syms {:id '_}) => {:id '_}
  (db-id-syms {:id 'hello}) => {:id '?hello}
  (db-id-syms {:id 12345}) => {:id 12345}"
  {:added "0.3"}
  [db]
  (if-let [id (and db (:id db))]
    (cond (symbol? id)
          (let [nid (cond (= id '_) '_
                          (.startsWith (name id) "?") id
                           :else (symbol (str "?" (name id))))]
            (assoc db :id nid))
          :else db)))

(defn wrap-db
  "allows the :db/id key to be used when specifying refs
  (normalise/normalise {:db/id 'hello
                        :account {:orders {:+ {:db/id '_
                                               :account {:user \"Chris\"}}}}}
                       {:schema (schema/schema examples/account-orders-items-image)}
                       {:normalise [wrap-db paths/wrap-plus]})
  => {:db {:id '?hello}
      :account {:orders {:+ {:db {:id '_} :account {:user \"Chris\"}}}}}"
  {:added "0.3"}
  [f]
  (fn [tdata tsch nsv interim fns env]
    (let [db (:db tdata)
          db (db-id-syms db)
          output (f (dissoc tdata :db) tsch nsv interim fns env)]
      (if db
        (assoc output :db db)
        output))))