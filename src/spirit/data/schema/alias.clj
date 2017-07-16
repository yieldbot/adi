(ns spirit.data.schema.alias
  (:require [hara.data.path :as data]))

(defn alias-attrs
  "standardises the template when using alias
  (alias-attrs  {:person/grandson [{:type :alias
                                    :alias {:ns :child/child
                                            :template {:child/son {}}}}]})
  => {:person/grandson [{:type :alias, :alias {:ns :child/child,
                                               :template {:child {:son {}}}}}]}"
  {:added "0.3"}
  [fschm]
  (reduce-kv (fn [out k [attr]]
               (if (= :alias (:type attr))
                 (assoc out k [(update-in attr [:alias :template]
                                          data/treeify-keys-nested)])
                 out))
             {}
             fschm))
