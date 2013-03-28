(ns adi.schema
  (:use [datomic.api :only [tempid]]
        [hara.control :only [if-let]]
        adi.utils)
  (:require [inflections.core :as inf])
  (:refer-clojure :exclude [if-let]))

;; ## Infer Methods

(defn infer-idents
  "Returns `sgeni` with the :ident keyword infer-idents will take a
   scheme-map and automatically adds the :ident keyword"
  [sgeni]

  (let [fsgeni (flatten-keys-in sgeni)
        add-ident (fn [[k [meta :as v]]]
                    (let [v-ident (->> (assoc meta :ident k)
                                       (assoc v 0 ))]
                      [k v-ident]))]
    (->> (map add-ident fsgeni)
         (into {})
         treeify-keys)))

(defn find-ref-idents [fsgeni]
  (->> fsgeni
       (filter (fn [[k [meta]]] (= :ref (:type meta))))
       (into {})))

(defn rev-ident? [k]
  (-> k keyword-val str (starts-with ":_")))

(defn find-reversible-ref-idents [fsgeni]
  (->> fsgeni
       (filter (fn [[k [meta]]]
                 (and (= :ref (:type meta))
                      (:ref-ns meta)
                      (keyword-ns (:ident meta))
                      (not (rev-ident? (:ident meta))))))
       (into {})))

(defn vec-reversible-lu [[meta]]
  (let [ident  (:ident meta)
        ref-ns (:ref-ns meta)]
    [(keyword-nsroot ident) ref-ns]))

(defn arrange-reversible-refs
  ([in] (arrange-reversible-refs in []))
  ([[refg & more] output]
     (if-let [[ks ms] refg]
       (cond (< 1 (count ms))
             (arrange-reversible-refs more
                               (concat output
                                       (map (fn [m] [(conj ks true) m]) ms)))
             :else
             (arrange-reversible-refs more
                               (conj output [(conj ks false) (first ms)])))
       output)))


(defn make-rev-meta-ident [[[root ref-ns many?] [meta]]]
  (let [ident  (:ident meta)
        ival    (keyword-val ident)
        rident (cond (= root ref-ns)
                     (let [rvec (concat (keyword-stemvec ident) '("referrers"))]
                       (keyword-join (cons "" rvec) "_"))

                     many?
                     (let [rvec (concat (keyword-stemvec ident)
                                        (list (->> root name inf/plural)))]
                       (keyword-join (cons "" rvec) "_"))

                     :else
                     (->> root name inf/plural (str "_")))]
    (keyword-join [ref-ns rident])))

(defn make-rev-meta [[[root ref-ns many] [meta] :as entry]]
  (let [ident  (:ident meta)
        ref-ns (:ref-ns meta)]
    [{:type        :ref
      :ref-key     (keyword-join [(keyword-ns ident)
                                  (str "_" (name (keyword-val ident)))])
      :ref-ns      (keyword-nsroot ident)
      :cardinality :many
      :ident       (make-rev-meta-ident entry)}]))

(defn infer-rev-refs
  "Returns `sgeni` with additional. Requires `sgeni` to already have been through `infer-ident`"
  [sgeni]
  (let [fsgeni (flatten-keys-in sgeni)
        rfs   (vals (find-reversible-ref-idents fsgeni))
        lus   (group-by vec-reversible-lu rfs)]
    (->> (arrange-reversible-refs (seq lus))
         (map make-rev-meta)
         (funcmap (fn [[meta]] (:ident meta)))
         (merge fsgeni)
         treeify-keys)))

(defn infer-all [sgeni]
  (-> sgeni infer-idents infer-rev-refs))

;; ## Schema Generation

(defn find-rev-idents [fgeni]
  (->> fgeni
       (filter (fn [[k [meta]]]
                 (and (= :ref (:type meta))
                      (rev-ident? (:ref-key meta)))))
       (into {})))

(defn remove-rev-idents [geni]
  (let [fgeni (flatten-keys-in geni)
        rvs   (keys (find-rev-idents fgeni))]
    (treeify-keys (apply dissoc fgeni rvs))))


(defn meta-property
  "Returns the keyword enumeration for datomic schemas properties.

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

(defn geni-property [meta k params res]
  (let [v (or (k meta) (:default params))
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
              (throw (Exception. (str "property " v " failed check"))))))))

(defn build-single-schema
  ([meta] (build-single-schema meta meta-geni {}))
  ([meta mg output]
     (if-let [[k v] (first mg)]
       (build-single-schema meta
                            (rest mg)
                            (geni-property meta k v output))
       (assoc output
         :db.install/_attribute :db.part/db
         :db/id (tempid :db.part/db)))))

(defn build-schema [geni]
  (let [metas (-> geni infer-all remove-rev-idents flatten-keys-in vals)]
    (map (fn [[v]] (build-single-schema v)) metas)))

(defn make-schema-rec [sgeni]
  (let [geni (infer-all sgeni)]
    {:geni  geni
     :fgeni (flatten-keys-in geni)}))

;; ## Geni Traversal

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
  ([fgeni] (find-keys fgeni :default true?))
  ([fgeni nss] (find-keys fgeni nss :default true?)))

(defn find-ref-keys
  ([fgeni] (find-keys fgeni :type :ref))
  ([fgeni nss] (find-keys fgeni nss :type :ref)))

; Infer Schema from Datomic Database

(defn infer-geni [conn])
