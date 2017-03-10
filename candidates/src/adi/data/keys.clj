(ns adi.data.keys
  (:require [hara.string.path :as path]))

(defn keyword-reverse
  "reverses the keyword by either adding or removing '_' in the value
  (keyword-reverse :a/b) => :a/_b
  (keyword-reverse :a/_b) => :a/b
  (keyword-reverse :b) => (throws Exception)
  "
  {:added "0.3"}
  [k]
  (if-let [kval (path/path-stem k)]
    (let [sval   (name kval)
          rsval  (if (.startsWith sval "_")
                   (.substring sval 1)
                   (str "_" sval))]
      (path/join [(path/path-ns k) rsval]))
    (throw (Exception. (str "Keyword " k " is not reversible.")))))

(defn keyword-reversed?
  "checks whether the keyword is reversed (begins with '_')
  (keyword-reversed? :a) => false
  (keyword-reversed? :a/b) => false
  (keyword-reversed? :a/_b) => true
  "
  {:added "0.3"}
  [k]
  (if-let [kval (-> k path/path-stem)]
    (-> kval name (.startsWith "_"))
    false))
