(ns adi.data.coerce
  (:require [clojure.edn :as edn]
            [ribol.core :refer [raise]]
            [hara.common :refer [error long? hash-set? hash-map? merge-nested]]))

(def date-format-json
  (java.text.SimpleDateFormat. "yyyy-MM-dd'T'HH:mm:ss'Z'"))

(defn assoc-set
  ([m s v]
     (if (hash-set? s)
       (apply assoc m (interleave s (repeat v)))
       (assoc m s v)))
  ([m s v & more]
     (let [out (assoc-set m s v)]
       (if more
         (apply assoc-set out more)
         out))))

(defn hash-mapset
  ([s v]
     (assoc-set {} s v))
  ([s v & more]
     (apply assoc-set {} s v more)))

(defn read-enum [s]
  (let [v (edn/read-string s)]
    (cond (or (keyword? v)
              (long v))
          v
          :else
          (error "READ_ENUM: " v " is not a proper :enum value"))))

(defn read-ref [s]
  (let [v (edn/read-string s)]
    (cond (or (long? v)
              (hash-map? v))
          v
          :else
          (error  "READ_REF: " v " is not a proper :ref value"))))

(def Numbers
  #{java.lang.Integer java.lang.Long java.lang.Float java.lang.Double clojure.lang.Ratio clojure.lang.BigInt java.math.BigDecimal})

(def Strings
  #{java.util.UUID java.net.URI})


(def from-string-chart
  {:keyword (fn [v] (keyword v))
   :bigint  (fn [v] (BigInteger. v))
   :bigdec  (fn [v] (BigDecimal. v))
   :long    (fn [v] (Long/parseLong v))
   :float   (fn [v] (Float/parseFloat v))
   :double  (fn [v] (Double/parseDouble v))
   :instant (fn [v] (.parse date-format-json v))
   :uuid    (fn [v] (hara.common/uuid v))
   :uri     (fn [v] (hara.common/uri v))
   :enum    (fn [v] (read-enum v))
   :ref     (fn [v] (read-ref v))})

(def default-coerce-to-string-chart
  (->> from-string-chart
       (map (fn [[k f]] [k {java.lang.String f}]))
       (into {})))

(def default-coerce-chart
  (merge-nested
   default-coerce-to-string-chart
   {:keyword
    (hash-mapset Strings           (fn [v] (keyword (str v)))
                 Numbers           (fn [v] (keyword (str v))))
    :string
    (hash-mapset java.util.Date (fn [v] (.format date-format-json v))
                 clojure.lang.Keyword (fn [v] (name v))
                 Strings (fn [v] (str v))
                 Numbers (fn [v] (str v)))
    :bigint
    (hash-mapset Numbers (fn [v] (bigint v)))
    :bigdec
    (hash-mapset Numbers (fn [v] (bigdec v)))
    :long
    (hash-mapset Numbers (fn [v] (long v))
                 java.util.Date (fn [v] (.getTime v)))
    :float
    (hash-mapset Numbers (fn [v] (float v)))
    :double
    (hash-mapset Numbers (fn [v] (double v)))
    :instant
    (hash-mapset Numbers (fn [v] (java.util.Date. (long v))))
    :enum
    (hash-mapset Numbers (fn [v] (long v)))}))

(def ^:dynamic *coerce-chart* default-coerce-chart)

(defn coerce
  ([v t]
     (coerce v t nil))
  ([v t chart]
     (if-let [c-fn (get-in (merge-nested *coerce-chart* chart) [t (type v)])]
       (c-fn v)
       (raise [:adi :coerce]
              (str "COERCE: Cannot coerce " v " from type " (type v) " to " t)))))
