(ns adi.schema.alias-test
  (:use midje.sweet)
  (:require [adi.schema.alias :refer :all]
            [adi.test.family :as family]
            [hara.data.path :as data]))

^{:refer adi.schema.alias/alias-attrs :added "0.3"}
(fact "standardises the template when using alias"
  (alias-attrs  {:person/grandson [{:type :alias
                                    :alias {:ns :child/child
                                            :template {:child/son {}}}}]})
  => {:person/grandson [{:type :alias, :alias {:ns :child/child,
                                               :template {:child {:son {}}}}}]})
