(ns adi.schema
  (:use [datomic.api :only [tempid]]
        adi.utils))

(defn get-meta [[v]] v)

(defn add-idents [geni]
  (let [fgeni (flatten-all-keys geni)
        add-ident (fn [[k v]]
                    (let [v-ident (->> (assoc (get-meta v) :ident k)
                                       (assoc v 0 ))]
                      [k v-ident]))]
    (->> (map add-ident fgeni)
         (into {})
         treeify-keys)))

(defn- meta-property [val ns]
  "Makes the keyword enumeration for datomic schemas properties.
   (meta-property :string :type) ;;=> :db.type/string"
  (keyword (str "db." (key-str ns) "/" (key-str val))))

(def meta-geni
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

(def geni-type-checks
  (let [types (-> meta-geni :type :check)]
    (zipmap types (map #(resolve (symbol (str (name %) "?"))) types))))

(defn- geni-property [fgeni k params res]
  (let [v (or (fgeni k) (:default params))
        geni-prop-pair  (fn [attr k v f]
                          (list (keyword (str "db/" (key-str attr)))
                                (f v k)))]
    (cond (nil? v)
          (if (:required params)
            (throw (Exception. (str "property " k " is required")))
            res)

          :else
          (let [chk  (or (:check params) (constantly true))
                f    (or (:fn params) (fn [x & xs] x))
                attr (or (:attr params) k)]
            (if (chk v)
              (apply assoc res (geni-prop-pair attr k v f))
              (throw (Exception. (str "vue " v " failed check"))))))))

(defn build-schema*
  ([lgeni] (build-schema* lgeni meta-geni {}))
  ([lgeni meta output]
     (if-let [[k v] (first meta)]
       (build-schema* lgeni
                      (rest meta)
                      (geni-property lgeni k v output))
       (assoc output
         :db.install/_attribute :db.part/db
         :db/id (tempid :db.part/db)))))

(defn build-schema [geni]
  (let [lgeni (-> geni add-idents flatten-all-keys vals)]
    (map (fn [[v]] (build-schema* v) ) lgeni)))

;;;;;

(defn find-keys
  ([fgeni meta-k meta-val]
     (find-keys fgeni (constantly true) meta-k meta-val))
  ([fgeni nss meta-k meta-cmp]
     (let [comp-fn  (fn [val cmp] (if (fn? cmp) (cmp val) (= val cmp)))
           filt-fn  (fn [k] (and (nss (key-ns k))
                                (comp-fn (meta-k (first (fgeni k))) meta-cmp)))]
       (set (filter filt-fn (keys fgeni))))))

(defn find-required-keys
  ([fgeni] (find-keys fgeni :required true))
  ([fgeni nss] (find-keys fgeni nss :required true)))

(defn find-default-keys
  ([fgeni] (find-keys fgeni :default (fn [x] (not (nil? x)))))
  ([fgeni nss] (find-keys fgeni nss :default (fn [x] (not (nil? x))))))

(defn find-ref-keys
  ([fgeni] (find-keys fgeni :type :ref))
  ([fgeni nss] (find-keys fgeni nss :type :ref)))
