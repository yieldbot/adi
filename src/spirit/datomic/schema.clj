(ns spirit.datomic.schema
  (:require [spirit.datomic.schema.generate :as generate]
            [spirit.datomic.schema.base :as base]
            [spirit.common.schema :as schema]))

(defn schema [schema]
  (schema/schema base/all-auto-defaults))

(defn ->datoms [schema]
  (generate/datomic-schema (:flat schema)))
