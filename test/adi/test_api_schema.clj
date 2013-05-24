(ns adi.test-api-schema
 (:use midje.sweet
       adi.utils
       adi.schema
       hara.common
       hara.hash-map
       hara.checkers)
 (:require [datomic.api :as d]
           [adi.core :as adi]))

(defn schema-properties [ds]
  (let [data (d/q '[:find ?ident ?type ?cardinality ?e :where
                    [?e :db/ident ?ident]
                    [?e :db/valueType ?t]
                    [?t :db/ident ?type]
                    [?e :db/cardinality ?c]
                    [?c :db/ident ?cardinality]]
                  (d/db (ds :conn)))]
    (zipmap (map first data) data)))

(defn schema-idents [ds]
  (d/q '[:find ?ident ?e :where
         [?e :db/ident ?ident]]
       (d/db (ds :conn))))

(defn make-enum [[ident id]]
  {:db/ident ident
   :db/id id})

(defn filter-keys [m nss]
  (cond (keyword? nss)
        (select-keys m [nss])

        (hash-set? nss)
        (select-keys m nss)))

(defn schema-enums
  ([ds]
     (let [data (schema-idents ds)]
       (treeify-keys
        (-> (zipmap (map first data)
                    (map make-enum data))
            (dissoc (keys (schema-properties ds)))))))
  ([ds nss]
     (filter-keys (schema-enums ds) nss)))

(defn schema
  ([ds]
      (treeify-keys
       (into {}
             (map (fn [[k [ident type car]]]
                    [ident [{:ident ident
                             :type (keyword-stem type)
                             :cardinality (keyword-stem car)}]])
                  (schema-properties ds)))))
  ([ds nss]
     (filter-keys (schema ds) nss)))

(defn schema-namespaces [ds]
  (keys (schema ds)))

(defn schema-enum-namespaces [ds]
  (keys (schema-enums ds)))


(fact
  (def ds (adi/datastore "datomic:mem://adi-test-api-schema" {}))

  (schema ds :db)
  =>
  (contains-in
   {:db {:noHistory
         [{:ident :db/noHistory,
           :type :boolean,
           :cardinality :one}],
         :cardinality
         [{:ident :db/cardinality,
           :type :ref,
           :cardinality :one}],
         :fulltext
         [{:ident :db/fulltext,
           :type :boolean,
           :cardinality :one}],
         :txInstant
         [{:ident :db/txInstant,
           :type :instant,
           :cardinality :one}],
         :excise
         [{:ident :db/excise,
           :type :ref,
           :cardinality :one}],
         :code
         [{:ident :db/code,
           :type :string,
           :cardinality :one}],
         :lang
         [{:ident :db/lang,
           :type :ref,
           :cardinality :one}],
         :unique
         [{:ident :db/unique,
           :type :ref,
           :cardinality :one}],
         :doc
         [{:ident :db/doc,
           :type :string,
           :cardinality :one}],
         :index
         [{:ident :db/index,
           :type :boolean,
           :cardinality :one}],
         :valueType
         [{:ident :db/valueType,
           :type :ref,
           :cardinality :one}],
         :fn
         [{:ident :db/fn,
           :type :fn,
           :cardinality :one}],
         :ident
         [{:ident :db/ident,
           :type :keyword,
           :cardinality :one}],
         :isComponent
         [{:ident :db/isComponent,
           :type :boolean,
           :cardinality :one}]}})

  (schema-namespaces ds)
  => (contains [:fressian :db.install :db.excise :db]
               :in-any-order)

  (schema-enum-namespaces ds)
  => (contains [:db.unique :db.install :db.type
                :db.lang :db.part :db.excise :db.cardinality
                :db.fn :fressian :db]
               :in-any-order)
  (schema-enums ds :db.type)
  =>
  (contains-in
   {:db.type {:boolean {:db/ident :db.type/boolean, :db/id 24},
              :float {:db/ident :db.type/float, :db/id 57},
              :string {:db/ident :db.type/string, :db/id 23},
              :ref {:db/ident :db.type/ref, :db/id 20},
              :instant {:db/ident :db.type/instant, :db/id 25},
              :uri {:db/ident :db.type/uri, :db/id 58},
              :bytes {:db/ident :db.type/bytes, :db/id 27},
              :keyword {:db/ident :db.type/keyword, :db/id 21},
              :bigint {:db/ident :db.type/bigint, :db/id 59},
              :bigdec {:db/ident :db.type/bigdec, :db/id 60}
              :uuid {:db/ident :db.type/uuid, :db/id 55},
              :double {:db/ident :db.type/double, :db/id 56},
              :fn {:db/ident :db.type/fn, :db/id 26},
              :long {:db/ident :db.type/long, :db/id 22}}})

  )



(keys (schema-enums ds))
'(:db.unique :db.install :db.type :db.lang :db.part :db.excise :db.cardinality :db.fn :fressian :db)

(schema-enums ds :db.lang)

(schema ds :db)
