(ns spirit.common
  (:require [hara.common.checks :refer [boolean?]]))

(def base
  {:ident        {:required true
                  :check keyword?}
   :type         {:required true
                  :default :string
                  :auto true}
   :cardinality  {:check #{:one :many}
                  :auto true
                  :default :one}
   :doc          {:check string?}
   :unique       {:check #{:value :identity}}
   :index        {:check boolean?}
   :required     {:check boolean?}
   :restrict     {:check ifn?}
   :default      {:check identity}})
