(ns adi.normalise.pipeline.ignore)

(defn wrap-nil-model-ignore
  "Simple Ignore
  (normalise/normalise {:account {:name \"Chris\"
                       :age 10
                       :parents [\"henry\" \"sally\"]}}
             {:schema (schema/schema examples/account-name-age-sex)})
  => nil #_(raises-issue {:key-path [:account],
                    :normalise true,
                    :nsv [:account :parents],
                    :no-schema true})


  (normalise/normalise {:account {:name \"Chris\"
                       :age 10
                       :parents [\"henry\" \"sally\"]}}
               {:schema (schema/schema examples/account-name-age-sex)
                :model {:ignore {:account {:parents :checked}}}}
               {:normalise-nil [wrap-nil-model-ignore]})
  => {:account {:name \"Chris\"
                :age 10
                :parents [\"henry\" \"sally\"]}}"
  {:added "0.3"} [f]
  (fn [subdata _ nsv interim env]
    (if (-> interim :ignore)
      subdata
      (f subdata nsv interim env))))
