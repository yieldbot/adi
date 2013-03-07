(ns adi.emit
  (:use adi.utils)
  (:require [datomic.api :as d]
            [adi.schema :as as]
            [adi.data :as ad]))

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
        rks   (filter #(= (-> % fschm first :type) :ref) ks)
        frks  (if nss (filter (fn [k] (some #(k-ns? k %) nss)) rks)
                rks)]
    (set frks)))

(defn emit-insert [data fschm]
  (let [pdata (ad/process data fschm)
        chdata (ad/characterise pdata fschm {:generate-ids true})]
    (ad/build chdata)))

(defn emit-update [data fschm]
  (let [pdata (ad/process data fschm {:add-defaults? false})
        chdata (ad/characterise pdata fschm {:generate-ids false})]
    (ad/build chdata)))

(defn emit-query [data fschm]
  (let [pdata (ad/process data fschm {:add-defaults? false :use-sets true})
        chdata (ad/characterise pdata fschm {:generate-syms true})]
    (ad/build-query chdata fschm)))
