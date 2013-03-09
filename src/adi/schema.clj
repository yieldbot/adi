(ns adi.schema
  (:use [datomic.api :only [tempid]]
        adi.utils))

(defn linearise-schm[schm]
  (let [fschm (flatten-all-keys schm)
        get-props (fn [[v]] v)]
    (map (fn [[k v]] (assoc (get-props v) :ident k)) fschm)))

(defn- meta-property [z ns]
  "Makes the keyword enumeration for datomic schemas properties.
   (meta-property :string :type) ;;=> :db.type/string"
  (keyword (str "db." (k-str ns) "/" (k-str z))))

(def meta-scheme-map
  {:ident        {:required true
                  :check keyword?}
   :type         {:required true
                  :check #{:keyword :string :boolean :long :bigint :float
                           :double :bigdec :ref :instant :uuid :uri :bytes}
                  :attr :valueType
                  :fn meta-property}
   :cardinality  {:check #{:one :many}
                  :default :one
                  :fn meta-property}
   :unique       {:check #{:value :identity}
                  :fn meta-property}
   :doc          {:check string?}
   :index        {:check boolean?}
   :fulltext     {:check boolean?}
   :component?   {:check keyword?}
   :no-history   {:check boolean?}})

(def type-checks
  (let [types (-> meta-scheme-map :type :check)]
    (zipmap types (map #(resolve (symbol (str (name %) "?"))) types))))

(defn- lschm-property-pair
  [attr k v f]
  (list (keyword (str "db/" (k-str attr)))
        (f v k)))

(defn- lschm-property [lschm k params res]
  (let [v (or (lschm k) (:default params))]
    (cond (nil? v)
          (if (:required params)
            (throw (Exception. (str "property " k " is required")))
            res)

          :else
          (let [chk  (or (:check params) (constantly true))
                f    (or (:fn params) (fn [x & xs] x))
                attr (or (:attr params) k)]
            (if (chk v)
              (apply assoc res (lschm-property-pair attr k v f))
              (throw (Exception. (str "vue " v " failed check"))))))))

(defn lschm->schema
  ([lschm] (lschm->schema lschm meta-scheme-map {}))
  ([lschm meta output]
     (if-let [[k v] (first meta)]
       (lschm->schema lschm
                      (rest meta)
                      (lschm-property lschm k v output))
       (assoc output
         :db.install/_attribute :db.part/db
         :db/id (tempid :db.part/db)))))

(defn required-keys [fschm nss]
  (filter (fn [k]
            (and (nss (k-ns k))
                 (:required (first (fschm k)))))
          (keys fschm)))
