(ns spirit.schema.alias-test
  (:use hara.test)
  (:require [spirit.schema.alias :refer :all]
            [spirit.test.family :as family]
            [hara.data.path :as data]))

^{:refer spirit.schema.alias/alias-attrs :added "0.3"}
(fact "standardises the template when using alias"
  (alias-attrs  {:person/grandson [{:type :alias
                                    :alias {:ns :child/child
                                            :template {:child/son {}}}}]})
  => {:person/grandson [{:type :alias, :alias {:ns :child/child,
                                               :template {:child {:son {}}}}}]})
