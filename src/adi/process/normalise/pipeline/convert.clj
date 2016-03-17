(ns adi.process.normalise.pipeline.convert
  (:require [hara.function.args :refer [op]]))

(defn wrap-single-model-convert
  "converts input according to model
 (normalise/normalise {:account/name \"Chris\"}
                     {:schema (schema/schema examples/account-name-age-sex)
                      :pipeline {:convert {:account {:name #(.toLowerCase %)}}}}
                     *wrappers*)
  => {:account {:name \"chris\"}}"
  {:added "0.3"}
  [f]
  (fn [subdata [attr] nsv interim fns adi]
    (let [trans-fn (:convert interim)
          nsubdata (if (fn? trans-fn)
                     (op trans-fn subdata adi)
                     subdata)]
      (f nsubdata [attr] nsv interim fns adi))))
