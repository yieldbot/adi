(ns spirit.common.pipeline.validate
  (:require [hara.event :refer [raise]]
            [hara.function.args :refer [op]]))

(defn wrap-single-model-validate
  "validates input according to model

 (pipeline/normalise {:account/name \"Chris\"}
                     {:schema (schema/schema examples/account-name-age-sex)
                      :pipeline {:validate {:account {:name number?}}}}
                     *wrappers*)
  => (throws-info {:not-validated true :nsv [:account :name]})

  (pipeline/normalise {:account/name \"Bob\"}
                       {:schema (schema/schema examples/account-name-age-sex)
                        :pipeline {:validate {:account {:name #(= % \"Bob\")}}}}
                       *wrappers*)
  => {:account {:name \"Bob\"}}"
  {:added "0.3"}
  [f]
  (fn [subdata [attr] nsv interim fns datasource]
    (let [subvalidate (:validate interim)]
      (cond (fn? subvalidate)
            (let [res (op subvalidate subdata datasource)
                  nsubdata (cond (or (true? res) (nil? res))
                                 subdata

                                :else
                                (raise [:normalise :not-validated
                                        {:data subdata
                                         :nsv nsv
                                         :key-path (:key-path interim)
                                         :validator subvalidate
                                         :error res}]))]
              (f nsubdata [attr] nsv interim fns datasource))

            :else
            (f subdata [attr] nsv interim fns datasource)))))
