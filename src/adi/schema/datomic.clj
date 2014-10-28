(ns adi.schema.datomic
  (:require [adi.schema.meta :as meta]
            [adi.schema.find :as find]
            [datomic.api :as datomic]
            [hara.string.path :as path]
            [hara.common.error :refer [error]]))

(defn datomic-attr-property
  "creates a property description from an adi one
  (datomic-attr-property {:type :string} :type
                         (meta/meta-schema :type) {})
 => {:db/valueType :db.type/string}

 (datomic-attr-property {:cardinality :one} :cardinality
                        (meta/meta-schema :cardinality) {})
 => {:db/cardinality :db.cardinality/one}

 (datomic-attr-property {} :cardinality
                        (meta/meta-schema :cardinality) {})
 => {:db/cardinality :db.cardinality/one}

 (datomic-attr-property {} :unique
                        (meta/meta-schema :unique) {})
 => {}
 "
  {:added "0.3"}
  [attr k mgprop res]
  (let [dft  (if (and (:default mgprop)
                      (:auto mgprop))
               (:default mgprop))
        v    (or (k attr) dft)
        prop-pair  (fn [attr k v f]
                     [(keyword (str "db/" (name attr)))
                      (f v k)])]
    (cond (nil? v)
          (if (:required mgprop)
            (error "DATOMIC-ATTR-PROPERTY: Property " k " is required")
            res)

          :else
          (let [chk  (or (:check mgprop) (constantly true))
                f    (or (:fn mgprop) (fn [x & xs] x))
                attr (or (:attr mgprop) k)]
            (if (not (chk v))
              (error  "DATOMIC-ATTR-PROPERTY: Property " v
                      " failed check on " attr " for check " chk)
              (apply assoc res (prop-pair attr k v f)))))))

(defn datomic-attr
  "creates a field description from a single adi attribute
  (datomic-attr [{:ident :name
                  :type  :string}])
  => (contains {:db.install/_attribute :db.part/db,
                :db/ident :name,
                :db/valueType :db.type/string,
                :db/cardinality :db.cardinality/one})

  (datomic-attr [{:ident       :account/tags
                  :type        :string
                  :cardinality :many
                  :fulltext    true
                  :index       true
                  :doc         \"tags for account\"}])
  => (contains {:db.install/_attribute :db.part/db
                :db/ident        :account/tags
                :db/index        true
                :db/doc          \"tags for account\"
                :db/valueType    :db.type/string
                :db/fulltext     true
                :db/cardinality  :db.cardinality/many})"
  {:added "0.3"}
  ([[attr]] (datomic-attr [attr] meta/meta-schema {}))
  ([[attr] mg output]
     (if-let [[k v] (first mg)]
       (recur [attr]
              (rest mg)
              (datomic-attr-property attr k v output))
       (assoc output
         :db.install/_attribute :db.part/db
         :db/id (datomic/tempid :db.part/db)))))

(defn datomic-enum
  "creates schema idents from the adi enum attr
  (->> (datomic-enum [{:ident   :person/gender
                       :type    :enum
                       :enum    {:ns     :person.gender
                                 :values #{:male  :female}}}])
       (map #(dissoc % :db/id)))
  => [{:db/ident :person.gender/female}
      {:db/ident :person.gender/male}]"
  {:added "0.3"}
  [[attr]]
  (map (fn [v]
         {:db/id (datomic/tempid :db.part/user)
          :db/ident (path/join [(-> attr :enum :ns) v])})
       (-> attr :enum :values)))

(defn remove-attrs [fschm f]
  (let [es (find/all-idents fschm f)]
    (apply dissoc fschm es)))

(defn datomic
  "creates a datomic-compatible schema from an adi one
  (->> (datomic {:node/male   [{:ident :node/male
                                :type  :ref
                                :ref {:ns  :node}}]
                 :person/gender [{:ident   :person/gender
                                  :type    :enum
                                  :enum    {:ns     :person.gender
                                            :values #{:male  :female}}}]})
       (map #(dissoc % :db/id)))
  => [{:db.install/_attribute :db.part/db,
       :db/cardinality :db.cardinality/one,
       :db/ident :node/male,
       :db/valueType :db.type/ref}
      {:db/ident :person.gender/female}
      {:db/ident :person.gender/male}]"
  {:added "0.3"}
  ([fschm]
     (let [attrs  (-> fschm
                      (remove-attrs find/is-reverse-ref?)
                      (remove-attrs (fn [attr] (#{:enum :alias} (:type attr))))
                      vals
                      (->> (map datomic-attr)))
           enums  (-> fschm
                      (find/all-attrs (fn [attr] (= :enum (:type attr))))
                      vals)
           enum-attrs (->> enums
                           (map #(assoc-in % [0 :type] :ref))
                           (map datomic-attr))
           enum-data  (mapcat datomic-enum enums)]
       (concat attrs enum-attrs enum-data)))
  ([fschm & more] (datomic (apply merge fschm more))))
