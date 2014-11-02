(ns adi.core
  (:require [datomic.api :as d]
            [hara.import :as im]
            [hara.common :refer [boolean? long? hash-map? keyword-join keyword-split
                                 hash-set? assoc-if assoc-nil]]
            [adi.schema :refer [make-xm ->datomic-schm]]
            [adi.model :as am]
            [adi.data :refer [query* datoms* normalise* characterise* pack* unpack*]]
            [ribol.core :refer [raise]]
            [clojure.walk :as wk]))

(im/import adi.common [iid isym incremental-sym-gen incremental-id-gen]
           adi.schema.types [db-id? entity? ref? enum?])

(def option-keys #{:ban-expressions :ban-ids :ban-top-id
                   :schema-required :schema-restrict :schema-defaults
                   :model-typecheck :model-coerce :skip-normalise
                   :skip-typecheck :first :ids
                   :generate-ids :generate-syms
                   :raw :simulate})

(defn- auto-pair-seq
  ([arr] (auto-pair-seq [] arr))
  ([output [x y & xs :as arr]]
     (cond (nil? x) output
           (or (and (option-keys x) (not (boolean? y)))
               (and (nil? y) (nil? xs)))
           (recur (conj output [x true]) (next arr))

           (and (option-keys x) (boolean? y))
           (recur (conj output [x y]) xs)

           :else (recur (conj output [x y]) xs))))

(defn- wrap-env-setup-db [f]
  (fn [env data]
    (let [db (or (:db env) (d/db (:conn env)))
          db (if-let [t (:at env)] (d/as-of db t) db)]
      (f (assoc env :db db) data))))

(defn- add-model-access [model access tsch]
  (cond (and (:allow model) (:pull model))
        model

        (and (not (:allow model)) (:pull model))
        (assoc model :allow (am/model-input access tsch))

        :else
        (let [imodel (am/model-input access tsch)
              rmodel (am/model-unpack imodel tsch)]
          (assoc-nil model :allow imodel :pull rmodel))))

(defn- add-model-pull [model pull tsch]
  (assoc model :pull
    (-> pull
        (am/model-input tsch)
        (am/model-unpack tsch))))

(defn- wrap-env-setup-model [f options]
  (fn [env data]
    (let [op  (:op env)
          model (or (:model env) (if op (-> env :profile op)))
          options (merge (:options env) (:options model) options)
          model  (if-let [access (:access env)]
                   (add-model-access model access (-> env :schema :tree))
                   model)
          model  (if-let [pull (:pull env)]
                   (add-model-pull model pull (-> env :schema :tree))
                   model)]
      (f (assoc env :model model) data))))

(defn- wrap-env-setup [f args]
  (fn [env data]
    (let [pargs (into {} (auto-pair-seq args))
          options (select-keys pargs option-keys)
          env-args (apply dissoc pargs option-keys)
          f  (wrap-env-setup-db f)
          f  (wrap-env-setup-model f options)]
      (f (-> env
             (assoc :data data)
             (merge env-args)
             (update-in [:options] merge options) )
         data))))

(defn setup-env [env args]
  ((wrap-env-setup (fn [env data] env) args) env nil))

(defn- wrap-select-data [f]
  (fn [env data]
    (cond (long? data)
          (if (or (-> env :options :ban-ids)
                  (-> env :options :ban-top-id))
            (raise [:id-banned {:data data}])
            #{data})

          (hash-map? data)
          (let [id  (or (get data :db/id)
                        (get-in data [:db :id]))
                qry (f env data)]
            (if (-> env :options :raw)
              #{qry}
              (let [res (->> (d/q qry (:db env))
                             (map first)
                             (set))]
                (if id
                  (if (get res id) #{id} #{})
                  res)))))))

(defn- wrap-select-pull [f ret-fn]
  (fn [env data]
    (let [res (f env data)]
      (cond (-> env :options :raw)
            res

            (-> env :options :first)
            (if-let [fst (first res)]
              (ret-fn fst))

            :else
            (set (map ret-fn res))))))

(defn- wrap-select-keyword [f]
  (fn [env data]
    (if (keyword? data)
      (if (-> env :options :ban-underscores)
        (raise [:keyword-banned {:data data}])
        (f env {data '_}))
      (f env data))))

(defn- wrap-select-set [f]
  (fn [env data]
    (if (hash-set? data)
      (set (mapcat #(f env %) data))
      (f env data))))

(defn- gen-query [env data]
  (let [env (-> env
                (update-in [:options]
                           dissoc
                           :schema-required
                           :schema-restrict
                           :schema-defaults)
                (assoc :type "query"))]
    (-> data
        (normalise* env)
        (pack* env)
        (characterise* env)
        (query* env))))

(defn- select-fn [env data op ret-fn]
  (let [sel-fn (-> gen-query
                   (wrap-select-data)
                   (wrap-select-keyword)
                   (wrap-select-set)
                   (wrap-select-pull ret-fn))]
    (sel-fn (assoc env :op op) data)))

(defn select-ids [env data & args]
  (let [env (setup-env env args)]
    (select-fn env data :select identity)))

(defn select-entities [env data & args]
  (let [env (setup-env env args)]
    (select-fn env data :select #(d/entity (:db env) %))))

(defn select [env data & args]
  (let [env (setup-env env args)
        view-fn #(unpack* (d/entity (:db env) %) env)]
    (select-fn env data :select view-fn)))

(defn- gen-datoms [env data]
  (let [env (-> env
                (assoc :type "datoms"))]
    (-> data
        (normalise* env)
        (pack* env)
        (characterise* env)
        (datoms* env))))

(defn- wrap-transaction-pull [f & [ids]]
  (fn [env data]
    (let [trs  (:transact env)
          dtms (f env data)]
      (cond (-> env :options :raw)
            dtms

            (-> env :options :simulate)
            (assoc env :db (:db-after (d/with (:db env) dtms)))

            :else
            (condp = trs
              nil      @(d/transact (:conn env) dtms)
              :promise (d/transact (:conn env) dtms)
              :async   (d/transact-async (:conn env) dtms)
              :full    (let [res @(d/transact (:conn env) dtms)
                             ndb (:db-after res)]
                         (if ids
                           (select-fn (assoc env :db ndb) ids :select
                                      #(unpack* (d/entity ndb %) env))
                           res))
              :compare (let [res @(d/transact (:conn env) dtms)
                             ndb (:db-after res)]
                         (if ids
                           [(select-fn env ids :select
                                       #(unpack* (d/entity (:db env) %) env))
                            (select-fn (assoc env :db ndb) ids :select
                                       #(unpack* (d/entity ndb %) env))]
                           res)))))))

(defn- wrap-vector-inserts [f]
  (fn [env data]
    (if (vector? data)
      (let [vfn (fn [env data]
                  (mapcat #(f env %) data))]
        (vfn env data))
      (f env data))))

(defn insert! [env data & args]
  (let [env (setup-env env args)
        in-fn (-> gen-datoms
                  (wrap-vector-inserts)
                  (wrap-transaction-pull))]
    (in-fn (assoc env :op :insert) data)))

(defn transact! [env data & args]
  (let [env (setup-env env args)
        in-fn (-> (fn [env data] data)
                  (wrap-transaction-pull))]
    (in-fn (assoc env :op :transact) data)))

(defn delete! [env data & args]
  (let [env (setup-env env args)
        ids (select-fn (assoc-in env [:options :raw] false)
                       data :delete identity)
        del-fn (-> (fn [env data]
                     (map (fn [x] [:db.fn/retractEntity x]) ids))
                   (wrap-transaction-pull ids))]
    (del-fn env data)))

(defn update! [env data update & args]
  (let [env (setup-env env args)
        ids (select-fn (assoc-in env [:options :raw] false)
                       data :modify identity)
        updates (mapv (fn [id] (assoc update :db/id id)) ids)
        env (if (-> env :options :ban-ids)
              (-> env
                  (assoc-in [:options :ban-ids] false)
                  (assoc-in [:options :ban-body-ids] true))
              env)
        in-fn (-> gen-datoms
                  (wrap-vector-inserts)
                  (wrap-transaction-pull ids))]
    (in-fn (assoc env :op :modify) updates)))


;; retractions

(defn- walk-path-gather [et attr]
  (let [rf (get et (-> attr :ref :key))]
    (cond (hash-set? rf) rf
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
                              (if (empty? more) [ets (keyword-join (conj ppath k))]
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
      (cond (hash-set? rf) (map #(f % retract) rf)
            :else [(f rf retract)]))))

(defn- retract-val [v retract]
  (cond (entity? v)
        (let [id (:db/id v)]
          (if (retract id) id))

        :else (if (retract v) v)))

(defn- make-entry-recs-single [et path retract]
  (let [vals (filter identity ((wrap-retract-val-set retract-val) et path retract))]
    (map (fn [v] [:db/retract (:db/id et) path v]) vals)))

(defn- make-entry-recs [ets entry env]
  (let [[path retract] (cond (keyword? entry) [entry (constantly true)]
                             (vector? entry) [(first entry) (second entry)]
                             :else (raise [:invalid-retraction-entry {:value entry}]))
        spath         (keyword-split path)
        _chk          (if-let [mod (-> env :model :allow)]
                        (if-not (get-in mod spath)
                          (raise [:model-path-access-not-allowed {:path spath}])))
        [ets path]    (walk-path ets spath

                                 (-> env :schema :tree))]
    (mapcat #(make-entry-recs-single % path retract) ets)))

(defn retract! [env data retracts & args]
  (let [env (setup-env env args)
        ids (select-fn (assoc-in env [:options :raw] false)
                       data :modify identity)
        ets (map #(d/entity (:db env) %) ids)
        data (mapcat #(make-entry-recs ets % env) retracts)
        rtr-fn (-> (fn [env data] data)
                   (wrap-transaction-pull ids))]
    (rtr-fn env data)))

;; - Model Walking

(declare linked-ids)

(defn- wrap-linked-set [f]
  (fn [etrf pmodel rmodel seen fsch]
    (cond (hash-set? etrf)
          (mapv #(f % pmodel rmodel seen fsch)
                (filter #(not (@seen (:db/id %))) etrf))
          :else
          (if (not (@seen (:db/id etrf)))
            (f etrf pmodel rmodel seen fsch)))))

(defn- linked-ids-loop
  ([et rmodel fsch]
     (linked-ids et rmodel rmodel (atom #{}) fsch))
  ([et pmodel rmodel seen fsch]
     (if-let [[k v] (first pmodel)]
       (do (cond (hash-map? v)
                 (if-let [etrf (get et (-> fsch k first :ref :key))]
                   ((wrap-linked-set linked-ids)
                    etrf v rmodel seen fsch))

                 (= v :yield)
                 (if-let [etrf (get et (-> fsch k first :ref :key))]
                   ((wrap-linked-set linked-ids)
                    etrf rmodel rmodel seen fsch)))
           (recur et (next pmodel) rmodel seen fsch)))))

(defn- linked-ids
  ([et rmodel fsch]
     (let [seen (atom #{})]
       (linked-ids et rmodel rmodel seen fsch)
       @seen))
  ([et pmodel rmodel seen fsch]
     (swap! seen conj (:db/id et))
     (linked-ids-loop et pmodel rmodel seen fsch)))


(defn- linked-entities [et rmodel env]
  (let [env (setup-env env [])]
    (map #(d/entity (:db env) %) (linked-ids et rmodel (-> env :schema :flat)))))

(defn delete-all! [env data & args]
  (let [env (setup-env env args)
        ids (select-fn (assoc-in env [:options :raw] false)
                       data :delete identity)
        rmodel (if-let [imodel (-> env :model :allow)]
                 (am/model-unpack imodel (-> env :schema :tree))
                 (raise :missing-allow-model))
        ets     (map #(d/entity (:db env) %) ids)
        all-ids (mapcat (fn [et]
                          (linked-ids et rmodel (-> env :schema :flat)))
                        ets)
        _ (println all-ids)
        output  (delete! env (set all-ids) :raw :ban-ids false :ban-top-id false)
        rtr-fn (-> (fn [env data] output)
                   (wrap-transaction-pull ids))]
    (rtr-fn env output)))


;; within-model updates - not secured

(defn- search-path-analysis
  ([spath tsch]
     (search-path-analysis spath tsch nil []))
  ([spath tsch last-res all-res]
     (if-let [[k v] (first spath)]
       (let [ks (keyword-split k)
             ks (if-let [nss (:ns last-res)]
                  (cons nss ks) ks)
             [attr] (get-in tsch ks)
             res  (-> (:ref attr)
                      (select-keys  [:val :rval :ns])
                      (assoc :term v))]
         (recur (next spath) tsch res (conj all-res res)))
       all-res)))

(defn- build-search-term [vterm all-res]
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

(defn- build-search-term-fn [all-res]
  (let [id (gensym)
        data (build-search-term id all-res)
        data (wk/postwalk (fn [x] (if (or (= x '_) (list? x))
                                   (list 'quote x)
                                   x))
                       data)]
    (eval (list 'fn [id] data))))

(defn update-in! [env data path update & args]
  (let [env (setup-env env args)
        _   (assert (even? (count path)) "The path must have a even number of items.")
        ids (select-fn (assoc-in env [:options :raw] false)
                       data :modify identity)
        spath (partition 2 path)
        svec  (search-path-analysis spath (-> env :schema :tree))
        ndata-fn (build-search-term-fn svec)
        last-ns (:ns (last svec))
        update (if last-ns {last-ns update} update)
        output  (mapcat
                 #(update! (dissoc env :model) (ndata-fn %) update
                           :raw :ban-body-ids false :ban-ids false :ban-top-id false) ids)
        rtr-fn (-> (fn [env data] data)
                   (wrap-transaction-pull ids))]
    (rtr-fn env output)))

(defn delete-in! [env data path & args]
  (let [env (setup-env env args)
        _   (assert (even? (count path)) "The path must have a even number of items.")
        ids (select-fn (assoc-in env [:options :raw] false)
                       data :modify identity)
        spath (partition 2 path)
        svec  (search-path-analysis spath (-> env :schema :tree))
        ndata-fn (build-search-term-fn svec)
        output  (mapcat
                 #(delete! (dissoc env :model) (ndata-fn %)
                           :raw :ban-body-ids false :ban-ids false :ban-top-id false) ids)
        rtr-fn (-> (fn [env data] data)
                   (wrap-transaction-pull ids))]
    (rtr-fn env output)))

;; - retract-in!

(defn- add-ns-entry [ns entry]
  (cond (vector? entry)
        [(keyword-join [ns (first entry)]) (second entry)]
        (keyword? entry) (keyword-join [ns entry])))

(defn retract-in! [env data path retracts & args]
  (let [env (setup-env env args)
        _   (assert (even? (count path)) "The path must have a even number of items.")
        ids (select-fn (assoc-in env [:options :raw] false)
                       data :modify identity)
        spath (partition 2 path)
        svec  (search-path-analysis spath (-> env :schema :tree))
        ndata-fn (build-search-term-fn svec)
        last-ns (:ns (last svec))
        nretracts (set (map #(add-ns-entry last-ns %) retracts))
        output  (mapcat
                 (fn [id] (retract! (dissoc env :model)
                               (ndata-fn id)
                               nretracts
                               :raw :ban-body-ids false :ban-ids false :ban-top-id false)) ids)
        rtr-fn (-> (fn [env data] data)
                   (wrap-transaction-pull ids))]
    (rtr-fn env output)))

;; - q and selectq

(defn- optional-vector-arg [opt args]
  (if (vector? opt)
    [opt args]
    [[] (cons opt args)]))

(defn- wrap-q-pull [f ret-fn]
  (fn [env data]
    (cond (-> env :options :raw)
          data

          :else
          (let [res (f env data)]
            (cond
             (-> env :options :first)
             (if-let [fst (first res)]
               (ret-fn fst))

             :else
             (set (map ret-fn res)))))))

(defn- q-fn [env data params? args ret-fn]
  (let [f (fn [env data] (apply d/q data (:db env) args))]
    ((wrap-q-pull f ret-fn) env data)))

(defn q [env data & [params? & args]]
  (let [[params? args] (optional-vector-arg params? args)
        env (setup-env env args)]
    (q-fn env data params? args (or (:q-fn env) identity))))

(defn selectq [env data & [params? & args]]
  (let [[params? args] (optional-vector-arg params? args)
        env (setup-env env args)
        view-fn #(unpack* (d/entity (:db env) (first %)) env)]
    (q-fn env data params? args view-fn)))

(defn selectq-entities [env data & [params? & args]]
  (let [[params? args] (optional-vector-arg params? args)
        env (setup-env env args)
        ent-fn (fn [v] (d/entity (:db env) (first v)))]
    (q-fn env data params? args ent-fn)))

(defn selectq-ids [env data & [params? & args]]
  (let [[params? args] (optional-vector-arg params? args)
        env (setup-env env args)]
    (q-fn env data params? args first)))

;; - Synchronised Transactions

(def transaction-ops
  #{#'transact! #'insert! #'update! #'delete! #'retract!
    #'retract-in! #'update-in! #'delete-in! #'delete-all!})

(defn- create-data-form [form env]
  (let [[f & args] form]
    (if (transaction-ops (resolve f))
      (concat (list f env) args (list :raw))
      (throw (AssertionError. (str "Only " transaction-ops " allowed."))))))

(defmacro sync-> [env args? & trns]
  (let [[args? trns] (optional-vector-arg args? trns)
        envsym (gensym)
        forms (filter identity (map #(create-data-form % envsym) trns))]
    `(let [~envsym  (setup-env ~env ~args?)]
       (transact! ~envsym (concat ~@forms)))))

;; - Environment Setup

(defn connect-env! [uri schema & [reset? no-install?]]
  (if reset? (do (d/delete-database uri)
                 (d/create-database uri)))
  (let [conn (try
               (d/connect uri)
               (catch clojure.lang.ExceptionInfo e
                 (when (= (ex-data e) {:db/error :peer/db-not-found})
                   (d/create-database uri)
                   (d/connect uri))))
        xm (make-xm schema)
        dschm (->datomic-schm xm)
        env {:conn conn :schema xm}]
    (if-not no-install?
      (d/transact conn dschm))
    env))

(defn install-env! [uri schema & [reset?]]
  (let [env (connect-env! uri schema reset?)]
    (d/release (:conn env))))

(defn disconnect-env! [env]
  (d/release (:conn env)))
