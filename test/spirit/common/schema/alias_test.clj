(ns spirit.common.schema.alias-test
  (:use hara.test)
  (:require [spirit.common.schema.alias :refer :all]
            [data.family :as family]
            [hara.data.path :as data]))

^{:refer spirit.common.schema.alias/alias-attrs :added "0.3"}
(fact "standardises the template when using alias"
  (alias-attrs  {:person/grandson [{:type :alias
                                    :alias {:ns :child/child
                                            :template {:child/son {}}}}]})
  => {:person/grandson [{:type :alias, :alias {:ns :child/child,
                                               :template {:child {:son {}}}}}]})
