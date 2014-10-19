(ns adi.test-is-component
  (:require [adi.core :as adi]
            [midje.sweet :refer :all]))

(def y {:thing [{:type :ref
                 :ref {:ns :books}
                 :cardinality :many}]
        :books {:id [{:type :uuid
                      :unique :value
                      :required true }]}})

(def dy (adi/datastore "datomic:mem://dy" y true true))

(def z {:thing [{:type :ref
                 :ref {:ns :books}
                 :cardinality :many
                 :isComponent true}]
        :books {:id [{:type :uuid
                      :unique :value
                      :required true}]}})

(def dz (adi/datastore "datomic:mem://dz" z true true))
