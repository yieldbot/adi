(ns adi.test-schema-initiation
  (:use midje.sweet
        adi.schema
        hara.common
        hara.hash-map)
  (:require [adi.core :as adi]
            [adi.api :as aa]))


(def lab-schema2 {:sample {:id [{:type :string
                                 :unique :ident}]}})

(def uri* "datomic:mem://adi-test-schema-initiation")
(def fgeni (infer-fgeni lab-schema2))

(fact
  (adi/datastore uri* lab-schema2)
  => (throws Exception)

  (aa/install-schema (:conn ds) fgeni)
  => (throws Exception)

  (emit-schema fgeni)
  => (throws Exception)

  (emit-single-schema (fgeni :sample/id))
  => (throws Exception)

  (verify fgeni)
  => (throws Exception))
