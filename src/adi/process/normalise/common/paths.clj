(ns adi.process.normalise.common.paths
  (:require [adi.process.normalise.base :as normalise]))

(defn wrap-plus
  "Allows additional attributes (besides the link :ns) to be added to the entity
  (normalise/normalise {:account {:orders {:+ {:account {:user \"Chris\"}}}}}
                       {:schema (schema/schema examples/account-orders-items-image)}
                       {:normalise [wrap-plus]})
  => {:account {:orders {:+ {:account {:user \"Chris\"}}}}}
  "
  {:added "0.3"}
  [f]
  (fn [tdata tsch nsv interim fns adi]
    (let [output (f (dissoc tdata :+) tsch nsv interim fns adi)
          pinterim  (normalise/submaps interim normalise/tree-directives :+)]
      (if-let [tplus (:+ tdata)]
        (let [pinterim (update-in pinterim [:key-path] conj :+)]
          (assoc output :+
                 ((:normalise fns) tplus (-> adi :schema :tree) [] pinterim fns adi)))
        output))))

(defn wrap-ref-path
  "Used for tracing the entities through `normalise`
  (normalise/normalise {:account {:orders {:+ {:account {:WRONG \"Chris\"}}}}}
                       {:schema (schema/schema examples/account-orders-items-image)}
                       {:normalise [wrap-ref-path wrap-plus]})

  => (raises-issue {:ref-path
                    [{:account {:orders {:+ {:account {:WRONG \"Chris\"}}}}}
                     {:account {:WRONG \"Chris\"}}]})"
  {:added "0.3"}
  [f]
  (fn [tdata tsch nsv interim fns adi]
    (f tdata tsch nsv (update-in interim [:ref-path] (fnil #(conj % tdata) [])) fns adi)))

(defn wrap-key-path
  "Used for tracing the keys through `normalise`
  (normalise/normalise {:account {:orders {:+ {:account {:WRONG \"Chris\"}}}}}
                       {:schema (schema/schema examples/account-orders-items-image)}
                       {:normalise [wrap-plus]
                        :normalise-branch [wrap-key-path]
                        :normalise-attr [wrap-key-path]})
  =>  (raises-issue {:key-path [:account :orders :+ :account]})"
  {:added "0.3"}
  [f]
  (fn [tdata tsch nsv interim fns adi]
    (f tdata tsch nsv (update-in interim [:key-path] (fnil #(conj % (last nsv)) [])) fns adi)))