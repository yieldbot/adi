(ns adi.data.normalise.ignore)

(defn wrap-nil-model-ignore [f]
  (fn [subdata nsv interim env]
    (if (-> interim :ignore)
      subdata
      (f subdata nsv interim env))))
