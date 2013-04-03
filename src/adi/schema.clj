(ns adi.schema
  (:use [datomic.api :only [tempid]]
        [hara.control :only [if-let]]
        adi.utils)
  (:require [inflections.core :as inf])
  (:refer-clojure :exclude [if-let]))


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
                    :check #{:keyword :string :boolean :long :bigint :float :enum
                             :double :bigdec :ref :instant :uuid :uri :bytes}
                    :default :string
                    :add? true
                    :attr :valueType
                    :fn meta-property}
     :cardinality  {:check #{:one :many}
                    :default :one
                    :add?    true
                    :fn meta-property}
     :unique       {:check #{:value :identity}
                    :fn meta-property}
     :doc          {:check string?}
     :index        {:check boolean?
                    :default false}
     :fulltext     {:check boolean?
                    :default false}
     :isComponent  {:check keyword?}
     :noHistory    {:check boolean?
                    :default false}})

(def geni-type-checks
  (let [types (-> meta-geni :type :check)]
    (zipmap types (map type-checker types))))

(defn get-defaults [[k mgprop]]
  (-> (select-keys mgprop [:default :add?])
      (assoc :id k)))

(def meta-geni-all-defaults
  (filter (fn [m] (-> m :default nil? not))
          (map get-defaults meta-geni)))

(def meta-geni-add-defaults
  (filter (fn [m] (and (-> m :default nil? not)
                      (-> m :add?)))
          (map get-defaults meta-geni)))

;; ## Infer Methods

(defn add-ident [[k [meta :as v]]]
  (let [v-ident (->> (assoc meta :ident k)
                     (assoc v 0))]
    [k v-ident]))

(defn infer-idents
  "Returns `sgeni` with the :ident keyword infer-idents will take a
   scheme-map and automatically adds the :ident keyword"
  [fsgeni]
  (->> (map add-ident fsgeni)
       (into {})))

(defn add-defaults [[k [meta :as v]] dfts]
  (let [mks   (map :id dfts)
        mdfts (map :default dfts)
        v-defaults (->> (merge (zipmap mks mdfts) meta)
                        (assoc v 0))]
    [k v-defaults]))

(defn infer-defaults
  ([fsgeni] (infer-defaults fsgeni meta-geni-add-defaults))
  ([fsgeni dfts]
     (->> (map #(add-defaults % dfts) fsgeni)
          (into {}))))

(defn flip-ident [k]
  (let [kval   (name (keyword-val k))
        rkval  (if (starts-with kval "_")
                 (.substring kval 1)
                 (str "_" kval))]
    (keyword-join [(keyword-ns k) rkval])))

(defn ident-reversed? [k]
  (-> k keyword-val str (starts-with ":_")))

(defn reversible-ref? [[meta]]
  (and (= :ref (:type meta))
       (-> meta :ref :ns)
       (not (-> meta :ref :norev))
       (keyword-ns (:ident meta))
       (not (ident-reversed? (:ident meta)))))

(defn find-refs [fsgeni]
  (->> fsgeni
       (filter (fn [[k [meta]]] (and (= :ref (:type meta))
                                    (-> meta :ref :norev not))))
       (into {})))

(defn find-ref-idents [fsgeni]
  (keys (find-refs fsgeni)))

(defn find-reversible-refs [fsgeni]
  (->> fsgeni
       (filter (fn [[k [meta]]] (reversible-ref? [meta])))
       (into {})))

(defn find-reversible-ref-idents [fsgeni]
  (keys (find-reversible-refs fsgeni)))

(defn vec-reversible-lu [[meta]]
  (let [ident  (:ident meta)
        ref-ns (->  meta :ref :ns)]
    [(keyword-nsroot ident) ref-ns]))

(defn determine-ref-rval [[[root ref-ns many?] [meta]]]
  (if-let [rval (-> meta :ref :rval)]
    rval
    (let [ident  (:ident meta)
          ival    (keyword-val ident)]
      (cond (= root ref-ns)
                       (let [rvec (concat (keyword-stemvec ident) '("referrers"))]
                         (keyword-join rvec "_"))

                       many?
                       (let [rvec (concat (keyword-stemvec ident)
                                          (list (->> root name inf/plural)))]
                         (keyword-join rvec "_"))

                       :else
                       (->> root name inf/plural keyword)))))

(defn determine-ref-meta [[meta]]
  (if-let [ident  (:ident meta)
           f-ref  (:ref meta)
           f-ns   (:ns f-ref)
           f-rval (:rval f-ref)]
    (let [n-ref {:type    :forward
                 :key     ident
                 :val     (keyword-val ident)
                 :rkey    (flip-ident ident)
                 :rident  (keyword-join [f-ns f-rval])}]
      [(assoc meta :ref (merge f-ref n-ref))])
    (error "Required keys: [ident, ref [ns rval]] " meta)))

(defn determine-revref-meta [[meta]]
  (if-let [ident   (:ident meta)
           f-ref   (:ref   meta)
           f-key   (:key    f-ref)
           f-val   (:val   f-ref)
           f-rkey  (:rkey  f-ref)
           f-rval  (:rval  f-ref)
           f-rident (:rident f-ref)]
    [{:ident       f-rident
       :cardinality :many
       :type        :ref
       :ref         {:ns      (keyword-nsroot ident)
                     :type    :reverse
                     :val     f-rval
                     :key     f-rkey
                     :rval    f-val
                     :rkey    f-key
                     :rident  ident}}]
    (error "Required keys: [ident, ref [key val rkey rval rident]" meta)))

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

(defn attach-ref-rval
  [[[root ref-ns many?] [meta] :as entry]]
  [(assoc-in meta [:ref :rval] (determine-ref-rval entry))])

(defn attach-ref-meta
  [[[root ref-ns many?] [meta] :as entry]]
  (determine-ref-meta (attach-ref-rval entry)))

(defn gather-reversible-refs [fsgeni]
  (let [rfs   (vals (find-reversible-refs fsgeni))
        lus   (group-by vec-reversible-lu rfs)]
    (->> (arrange-reversible-refs (seq lus))
         (map attach-ref-meta))))

(defn infer-refs [fsgeni]
  (let [rfcoll  (gather-reversible-refs fsgeni)
        revcoll (->> rfcoll
                     (filter reversible-ref?)
                     (map determine-revref-meta))
        get-ident (fn [[meta]] (:ident meta))]
    (merge fsgeni
           (funcmap get-ident rfcoll)
           (funcmap get-ident revcoll))))


(defn find-revrefs [fgeni]
  (->> fgeni
       (filter (fn [[k [meta]]]
                 (and (= :ref (:type meta))
                      (= :reverse (-> meta :ref :type)))))
       (into {})))

(defn find-revref-idents [fgeni]
  (keys (find-revrefs fgeni)))

(defn remove-revrefs [fgeni]
  (let [rvs   (find-revref-idents fgeni)]
    (apply dissoc fgeni rvs)))

(defn make-ref-lu [fgeni ks]
  (let [get-rident (fn [k] (-> fgeni k first :ref :rident))]
    (zipmap ks (map get-rident ks))))

(defn make-lu
  ([fgeni] (make-lu fgeni {}))
  ([fgeni output]
     (if-let [[k [meta]] (first fgeni)]
       (cond (and (= :ref (:type meta))
                  (= :reverse (-> meta :ref :type)))
             (recur (next fgeni) (assoc output (-> meta :ref :key) k k k))
             :else
             (recur (next fgeni) (assoc output k k)))
       output)))

(defn infer-fgeni [sgeni]
  (-> (flatten-keys-in sgeni) infer-idents infer-defaults infer-refs))

(defn make-scheme-model [sgeni]
  (let [fgeni (infer-fgeni sgeni)]
    {:geni  (treeify-keys fgeni)
     :fgeni fgeni
     :lu    (make-lu fgeni)}))


;;; ## emit-schema

(defn make-enum-rel [[enmeta]]
  (-> enmeta
      (assoc :type :ref)
      (assoc :ref (:enum enmeta))
      (dissoc :enum)
      (assoc-in [:ref :type] :enum-rel)
      vector))

(defn find-enums [fgeni]
  (->> fgeni
       (filter (fn [[k [meta]]] (= :enum (:type meta))))
       (into {})))

(defn find-enum-idents [fgeni]
  (keys (find-enums fgeni)))

(defn remove-enums [fgeni]
  (let [es   (find-enums fgeni)
        enums  (vals es)
        e-rels (map make-enum-rel enums)
        mfgeni (apply dissoc fgeni (keys es))
        get-ident (fn [[meta]] (:ident meta))]
    (merge mfgeni
           (funcmap get-ident e-rels))))

(defn emit-schema-property [meta k mgprop res]
  (let [dft  (if-let [dft (:default mgprop)
                      add? (:add? mgprop)]
               dft)
        v    (or (k meta) dft)
        prop-pair  (fn [attr k v f]
                     [(keyword (str "db/" (keyword-str attr)))
                      (f v k)])]
    (cond (nil? v)
          (if (:required mgprop)
            (error "property " k " is required")
            res)

          :else
          (let [chk  (or (:check mgprop) (constantly true))
                f    (or (:fn mgprop) (fn [x & xs] x))
                attr (or (:attr mgprop) k)]
            (if (chk v)
              (apply assoc res (prop-pair attr k v f))
              (error "property " v " failed check"))))))

(defn emit-single-schema
  ([[meta]] (emit-single-schema [meta] meta-geni {}))
  ([[meta] mg output]
     (if-let [[k v] (first mg)]
       (emit-single-schema [meta]
                           (rest mg)
                           (emit-schema-property meta k v output))
       (assoc output
         :db.install/_attribute :db.part/db
         :db/id (tempid :db.part/db)))))

(defn emit-enum-val-schemas
  ([[enmeta]]
     (map (fn [v]
            {:db/id (tempid :db.part/user)
             :db/ident (keyword-join [(-> enmeta :enum :ns) v])})
          (-> enmeta :enum :values))))

(defn emit-schema
  ([fgeni]
     (let [enums  (vals (find-enums fgeni))
           metas  (-> fgeni remove-revrefs remove-enums vals)]
       (concat
        (map emit-single-schema metas)
        (mapcat emit-enum-val-schemas enums))))
  ([fgeni & more] (emit-schema (apply merge fgeni more))))

;; ## Geni Search

(defn find-keys
  ([fgeni mk mv]
     (find-keys fgeni (constantly true) mk mv))
  ([fgeni nss mk mv]
     (let [comp-fn  (fn [val cmp] (if (fn? cmp) (cmp val) (= val cmp)))
           filt-fn  (fn [k] (and (nss (keyword-ns k))
                                (comp-fn (mk (first (fgeni k))) mv)))]
       (set (filter filt-fn (keys fgeni)))))
  ([fgeni nss mk mv mk1 mv1 & more]
     (let [ks (find-keys fgeni nss mk mv)
           nfgeni (select-keys fgeni ks)]
       (apply find-keys nfgeni nss mk1 mv1 more))))

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

(defn propose-geni [conn])
