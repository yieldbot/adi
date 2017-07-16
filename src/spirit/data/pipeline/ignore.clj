(ns spirit.data.pipeline.ignore)

(defn wrap-nil-model-ignore
  "wraps the normalise-nil function such that any unknown keys are ignored
  (pipeline/normalise {:account {:name \"Chris\"
                       :age 10
                       :parents [\"henry\" \"sally\"]}}
               {:schema (schema/schema examples/account-name-age-sex)
                :pipeline {:ignore {:account {:parents :checked}}}}
               {:normalise-nil [ignore/wrap-nil-model-ignore]})
  => {:account {:name \"Chris\"
                :age 10
                :parents [\"henry\" \"sally\"]}}
  "
  {:added "0.3"}
  [f]
  (fn [subdata _ nsv interim datasource]
    (if (-> interim :ignore)
      subdata
      (f subdata nil nsv interim datasource))))
