(ns adi.emit
  (:use adi.utils)
  (:require [datomic.api :as d]
            [adi.schema :as as]))

(defn emit-schema
  "Generates all schemas using a datamap that can be installed
   in the datomic database."
  ([schm] 
    (->> (as/linearise-schm schm)
         (map as/lschm->schema)))
  ([schm & schms] (emit-schema (apply merge schm schms))))

(defn emit-refroute [schm & [nss]]
  (let [fschm (flatten-keys schm)
        ks    (keys fschm)
        rks   (filter #(= (-> fschm first :type) :ref) ks)]
    (set rks)))