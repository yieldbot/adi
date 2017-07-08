(ns spirit.common.http.transport)

(def ^:dynamic *default-config*
  {:protocol "http"
   :host     "localhost"
   :port     8000
   :format   :edn})

(defmulti write-value
  "writes the string value of the datastructure according to format
 
   (write-value {:a 1} :edn)
   => \"{:a 1}\""
  {:added "0.5"}
  (fn [s format] format))

(defmethod write-value :edn
  [s _]
  (pr-str s))

(defmulti read-value
  "read the string value of the datastructure according to format
 
   (read-value \"{:a 1}\" :edn)
   => {:a 1}"
  {:added "0.5"}
  (fn [s format] format))

(defmethod read-value :edn
  [s _]
  (read-string s))

(defprotocol IConnection
  (-push    [conn {:keys [id header body]} opts])
  (-request [conn {:keys [id header body]} opts]))

(defmulti read-body
  "reads the body of the request can be expanded
 
   (read-body \"{:a 1}\" :edn)
   => {:a 1}"
  {:added "0.5"}
  (fn [body format] (type body)))

(defmethod read-body nil
  [body _]
  {})

(defmethod read-body String
  [body format]
  (read-value body format))

(defn wrap-handler
  "wraps a handler given a lookup and a config
 
   ((wrap-handler identity
                   [:add-number
                    :mul-number]
                   {:ops {:add 5 :mul 10}}         
                   {:add-number {:func (fn [handler arg]
                                         (fn [number]
                                          (+ (handler number) arg)))
                                 :args [[:ops :add]]}
                    :mul-number {:func (fn [handler arg]
                                         (fn [number]
                                           (* (handler number) arg)))
                                 :args [[:ops :mul]]}})
    10)
   => 150"
  {:added "0.5"}
  [handler wrappers config lookup]
  (reduce (fn [out k]
            (let [{:keys [func args]} (get lookup k)]
              (if-not func (throw (ex-info (str "Missing function for key") {:key k})))
              (apply func out (map #(get-in config %) args))))
          handler
          wrappers))
