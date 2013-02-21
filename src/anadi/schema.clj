(ns anadi.schema
  (:use [datomic.api :only [tempid]]
        anadi.utils))

(defn- enum-property [val kns]
  "Makes the keyword enumeration for datomic schemas properties.
   (enum-property :string :type) ;;=> :db.type/string"
  (keyword (str "db." (key-str kns) "/" (key-str val))))

(def meta-schema
  {:ident        {:required true
                  :check keyword?}
   :type         {:required true
                  :check #{:keyword :string :boolean :long :bigint :float
                           :double :bigdec :ref :instant :uuid :uri :bytes}
                  :attr :valueType
                  :fn enum-property}
   :cardinality  {:check #{:one :many}
                  :default :one
                  :fn enum-property}
   :unique       {:check #{:value :identity}
                  :fn enum-property}
   :doc          {:check string?}
   :index        {:check boolean?}
   :fulltext     {:check boolean?}
   :component?   {:check keyword?}
   :no-history   {:check boolean?}})

(def type-checks
  (let [types (-> meta-schema :type :check)]
    (zipmap types (map #(resolve (symbol (str (name %) "?"))) types))))


(defn- linearise [dm]
  (let [fm (flatten-keys dm)
        get-props (fn [[v]] v)]
    (map (fn [[k v]] (assoc (get-props v) :ident k)) fm)))


(defn- property-pair
  [attr k val f]
  (list (keyword (str "db/" (key-str attr)))
        (f val k)))

(defn- schema-property [prm kns params res]
  (let [val (or (prm kns) (:default params))]
    (cond (nil? val)
          (if (:required params)
            (throw (Exception. (str "property " kns " is required")))
            res)

          :else
          (let [chk  (or (:check params) (constantly true))
                f    (or (:fn params) (fn [x & xs] x))
                attr (or (:attr params) kns)]
            (if (chk val)
              (apply assoc res (property-pair attr kns val f))
              (throw (Exception. (str "value " val " failed check"))))))))

(defn- ->schema
  ([prm] (->schema prm meta-schema {}))
  ([prm meta output]
     (if-let [[k v] (first meta)]
       (->schema prm
                   (rest meta)
                   (schema-property prm k v output))
       (assoc output
         :db.install/_attribute :db.part/db
         :db/id (tempid :db.part/db)))))

(defn generate-schemas
  "Generates all schemas using a datamap that can be installed
   in the datomic database."
  ([dm] (map ->schema (linearise dm)))
  ([dm & dms] (generate-schemas (apply merge dm dms))))
