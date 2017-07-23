(ns spirit.network.common)

(defmulti read-value  (fn [string format] format))

(defmulti write-value (fn [data format] format))

(defmethod read-value :edn
  [string _]
  (read-string string))

(defmethod write-value :edn
  [data _]
  (pr-str data))

(defn pack [{:keys [format]}
            {:keys [id type status params data] :as package}]
  (try
    (write-value package format)
    (catch Exception e
      (write-value {:id id
                    :type   :error
                    :input  {:type type
                             :data data
                             :params params}
                    :data   {:message (ex-info e)}} format))))

(defn unpack [{:keys [format]} message]
  (try
    (read-value message format)
    (catch Exception e
      {:id     :error/tansport
       :type   :error
       :input  {:string message}
       :data   {:message (ex-info e)}} format)))
