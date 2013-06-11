(ns adi.emit.coerce)

(def date-format-json
  (java.text.SimpleDateFormat. "yyyy-MM-dd'T'HH:mm:ss'Z'"))

(def coerce-chart
  {"json" {java.lang.String
           {:keyword (fn [v] (keyword v))
            :boolean (fn [v] (read-string v))
            :long    (fn [v] (Long/parseLong v))
            :float   (fn [v] (Float/parseFloat v))
            :double  (fn [v] (Double/parseDouble v))
            :bigint  (fn [v] (BigInteger. v))
            :bigdec  (fn [v] (BigDecimal. v))
            :enum    (fn [v] (keyword v))
            :instant (fn [v] (.parse date-format-json v))
            :uuid    (fn [v] (hara.common/uuid v))
            :uri     (fn [v] (hara.common/uri v))}
           java.lang.Double
           {:float (fn [v] (long v))
            :bigdec (fn [v] (bigdec v))
            :string (fn [v] (str v))}
           java.lang.Long
           {:bigint (fn [v] (bigint v))
            :enum   (fn [v] v)
            :string (fn [v] (str v))}}})

(defn coerce [format v t]
  (if-let [c-fn (get-in coerce-chart [format (type v) t])]
    (c-fn v)))
