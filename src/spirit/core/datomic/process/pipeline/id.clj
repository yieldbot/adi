(ns spirit.core.datomic.process.pipeline.id
  (:require [hara.event :refer [raise]]
            [hara.common.checks :refer [long?]]
            [spirit.core.datomic.data.checks :as checks]))

(defn wrap-single-id
  {:added "0.3"}
  [f]
  (fn [subdata [attr] nsv interim fns datasource]
    (if (and (= (:type attr) :ref)
             (or (long? subdata) 
                 (checks/db-id? subdata)))
      subdata
      (f subdata [attr] nsv interim fns datasource))))
