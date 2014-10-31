(ns adi.core.transaction
  (:require [hara.common.checks :refer [hash-map? long?]]
            [hara.data.nested :refer [merge-nil-nested]]
            [adi.data
             [common :refer [iid-seed]]
             [checks :refer [db-id?]]]
            [adi.core
             [prepare :as prepare]
             [select :as select]
             [model :as model]
             [link :as link]
             [depack :as depack]]
            [adi.process
             [normalise :as normalise]
             [pack :as pack]
             [unpack :as unpack]
             [emit :as emit]]
            [ribol.core :refer [raise]]
            [datomic.api :as datomic]
            [clojure.walk :as walk]))

(defn gen-datoms [adi]
  (let [data (-> adi :process :input)]
    (-> adi
        (assoc :type "datoms")
        (normalise/normalise)
        (pack/pack)
        (emit/emit))))
