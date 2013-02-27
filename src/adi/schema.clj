(ns adi.schema
  (:use [datomic.api :only [tempid]]
        adi.utils))

(defn- enum-property [val kns]
  "Makes the keyword enumeration for datomic schemas properties.
   (enum-property :string :type) ;;=> :db.type/string"
  (keyword (str "db." (key-str kns) "/" (key-str val))))

(def meta-scheme-map
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
  (let [types (-> meta-scheme-map :type :check)]
    (zipmap types (map #(resolve (symbol (str (name %) "?"))) types))))

(defn- linearise [sm]
  (let [fsm (flatten-keys sm)
        get-props (fn [[v]] v)]
    (map (fn [[k v]] (assoc (get-props v) :ident k)) fsm)))

(defn- sm-property-pair
  [attr k val f]
  (list (keyword (str "db/" (key-str attr)))
        (f val k)))

(defn- sm-property [lsm kns params res]
  (let [val (or (lsm kns) (:default params))]
    (cond (nil? val)
          (if (:required params)
            (throw (Exception. (str "property " kns " is required")))
            res)

          :else
          (let [chk  (or (:check params) (constantly true))
                f    (or (:fn params) (fn [x & xs] x))
                attr (or (:attr params) kns)]
            (if (chk val)
              (apply assoc res (sm-property-pair attr kns val f))
              (throw (Exception. (str "value " val " failed check"))))))))

(defn- lsm->schema
  ([lsm] (lsm->schema lsm meta-scheme-map {}))
  ([lsm meta output]
     (if-let [[k v] (first meta)]
       (lsm->schema lsm
                   (rest meta)
                   (sm-property lsm k v output))
       (assoc output
         :db.install/_attribute :db.part/db
         :db/id (tempid :db.part/db)))))

(defn emit-schema
  "Generates all schemas using a datamap that can be installed
   in the datomic database."
  ([sm] 
    (->> (linearise sm)
         (map lsm->schema)))
  ([sm & sms] (emit-schema (apply merge sm sms))))

(defn rset [sm]
  (let [fsm (flatten-keys sm)
        ks (keys fsm)
        rks (filter #(= (-> fsm first :type) :ref) ks)]
    (set rks)))