(ns adi.data.normalise.convert
  (:require [hara.common :refer [op]]))

(defn wrap-single-model-convert [f]
  (fn [subdata [attr] nsv interim fns env]
    (let [trans-fn (:convert interim)
          nsubdata (if (fn? trans-fn)
                     (op trans-fn subdata env)
                     subdata)]
      (f nsubdata [attr] nsv interim fns env))))
