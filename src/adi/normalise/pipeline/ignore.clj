(ns adi.normalise.pipeline.ignore)

(defn wrap-nil-model-ignore
  "wraps the normalise-nil function such that any unknown keys are ignored
  (normalise/normalise {:account {:name \"Chris\"
                       :age 10
                       :parents [\"henry\" \"sally\"]}}
               {:schema (schema/schema examples/account-name-age-sex)
                :model {:ignore {:account {:parents :checked}}}}
               {:normalise-nil [ignore/wrap-nil-model-ignore]})
  => {:account {:name \"Chris\"
                :age 10
                :parents [\"henry\" \"sally\"]}}
  "
  {:added "0.3"}
  [f]
  (fn [subdata _ nsv interim env]
    (if (-> interim :ignore)
      subdata
      (f subdata nsv interim env))))
