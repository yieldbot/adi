(ns adi.schema
  (:use [datomic.api :only [tempid]]
        adi.utils)
  (:require [inflections.core :as inf]))

(defn- get-meta [[v]] v)

(defn infer-idents
  "Returns `geni` with the :ident keyword infer-idents will take a
   scheme-map and automatically adds the :ident keyword"
  [geni]

  (let [fgeni (flatten-keys-in geni)
        add-ident (fn [[k v]]
                    (let [v-ident (->> (assoc (get-meta v) :ident k)
                                       (assoc v 0 ))]
                      [k v-ident]))]
    (->> (map add-ident fgeni)
         (into {})
         treeify-keys)))


(def s0-geni
  (infer-idents {:account {:name  [{:type    :string}]
                           :email [{:type    :ref
                                    :ref-ns  :email}]}
                 :email   {:field [{:type    :string}]}}))


(defn find-ref-idents [fgeni]
  (->> fgeni
       (filter (fn [[k [meta]]] (= :ref (:type meta))))
       (into {})))

(defn find-rev-idents [fgeni]
  (->> fgeni
       (filter (fn [[k [meta]]]
                 (and (= :ref (:type meta))
                      (-> (:ref-key meta)
                          keyword-val
                          str
                          (starts-with ":_")))))
       (into {})))

(defn remove-reverse-idents [geni]
  (let [fgeni (flatten-keys-in geni)
        rvs   (keys (find-reverse-idents fgeni))]
    (treeify-keys (apply dissoc fgeni rvs))))


(defn infer-reverse-ref [])

(defn lu-ns [[meta]]
  (let [ident  (:ident meta)
        ref-ns (:ref-ns meta)]
    [(keyword-nsroot ident) ref-ns]))

(defn make-reverse-meta-single [[meta] short?]
  (let [ident  (:ident meta)
        ref-ns (:ref-ns meta)]
    [{:type        :ref
      :ref-key     (keyword-join [(keyword-ns ident)
                                  (str "_" (name (keyword-val ident)))])
      :ref-ns      (keyword-nsroot ident)
      :cardinality :many
      :ident      (if short?
                    (keyword-join [ref-ns (inf/plural (->> ident keyword-nsroot name (str "_")))])
                    (keyword-join [ref-ns
                                   (keyword-join
                                    (cons "" (reverse (keyword-split ident))) "_")]))}]))

(defn infer-reverse-refs
  "Returns `geni` with additional. Requires `geni` to already have been through `infer-ident`"
  [geni]
  (let [fgeni (flatten-keys-in geni)
        rfs   (vals (find-ref-idents fgeni))
        lus   (group-by lu-ns rfs)]
    [rfs lus]
    ;; group-by :root-ns and :ref-ns
    ))

(keyword-join [""  "oeuo" "OEuoe"] "_")


(infer-reverse-refs s0-geni)








(defn- meta-property
  "Returns the keyword enumeration for datomic schemas properties.

      (meta-property :string :type)
      ;=> :db.type/string
  "
  [val ns]
  (keyword (str "db." (keyword-str ns) "/" (keyword-str val))))







(comment
  (defn get-meta [[v]] v)

  (defn infer-idents [geni]
    (let [fgeni (flatten-keys-in geni)
          add-ident (fn [[k v]]
                      (let [v-ident (->> (assoc (get-meta v) :ident k)
                                         (assoc v 0 ))]
                        [k v-ident]))]
      (->> (map add-ident fgeni)
           (into {})
           treeify-keys)))

  (defn meta-property
    "Makes the keyword enumeration for datomic schemas properties.

      (meta-property :string :type)
      ;=> :db.type/string
    "
    [val ns]
    (keyword (str "db." (keyword-str ns) "/" (keyword-str val))))

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
                            (list (keyword (str "db/" (keyword-str attr)))
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
    (let [lgeni (-> geni infer-idents flatten-keys-in vals)]
      (map (fn [[v]] (build-schema* v) ) lgeni)))

;;;;;

  (defn find-keys
    ([fgeni meta-k meta-val]
       (find-keys fgeni (constantly true) meta-k meta-val))
    ([fgeni nss meta-k meta-cmp]
       (let [comp-fn  (fn [val cmp] (if (fn? cmp) (cmp val) (= val cmp)))
             filt-fn  (fn [k] (and (nss (keyword-ns k))
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

;;;;;;

  (defn build-geni [])
)
