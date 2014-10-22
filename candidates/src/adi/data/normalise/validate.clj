(ns adi.data.normalise.validate
  (:require [hara.common :refer [op]]
            [ribol.core :refer [raise]]))

(defn wrap-single-model-validate [f]
  (fn [subdata [attr] nsv interim fns env]
    (let [subvalidate (:validate interim)]
      (cond (fn? subvalidate)
            (let [res (op subvalidate subdata env)
                  nsubdata (cond (or (true? res) (nil? res))
                                 subdata

                                :else
                                (raise [:adi :normalise :not-validated
                                        {:data subdata
                                         :nsv nsv
                                         :key-path (:key-path interim)
                                         :validator subvalidate
                                         :error res}]))]
              (f nsubdata [attr] nsv interim fns env))

            :else
            (f subdata [attr] nsv interim fns env)))))