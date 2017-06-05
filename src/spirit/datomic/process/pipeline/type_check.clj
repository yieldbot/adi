(ns spirit.datomic.process.pipeline.common.type-check
  (:require [spirit.common.data.coerce :refer [coerce]]
            [spirit.common.schema.base :as base]
            [spirit.datomic.schema.base]
            [hara.event :refer [raise]]))

(defn wrap-single-type-check
  "wraps normalise to type check inputs as well as to coerce incorrect inputs
  (pipeline/normalise {:account {:age \"10\"}}
                       {:schema (schema/schema examples/account-name-age-sex)}
                       {:normalise-single [wrap-single-type-check]})
  => (throws-info {:type :long,
                    :data \"10\",
                    :wrong-type true})

  (pipeline/normalise {:account {:age \"10\"}}
                       {:schema (schema/schema examples/account-name-age-sex)
                        :options {:use-coerce true}}
                       {:normalise-single [wrap-single-type-check]})
  => {:account {:age 10}}"
  {:added "0.3"}
  [f]
  (fn [subdata [attr] nsv interim fns spirit]
    
    (let [t (:type attr)
          chk (base/type-checks t)]
      (cond
        (chk subdata) (f subdata [attr] nsv interim fns spirit)

        (-> spirit :options :use-coerce)
        (f (coerce subdata t) [attr] nsv interim fns spirit)
        
        :else
        (raise [:normalise :wrong-type
                {:data subdata :nsv nsv :key-path (:key-path interim) :type t}]
               (str "WRAP_SINGLE_TYPE_CHECK: " subdata " in " nsv " is not of type " t))))))
