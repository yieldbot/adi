(ns adi.schema
  (:require [hara.import :as im]
            [adi.schema.emit :refer [emit-dschm]]))

(im/import adi.schema.xm [make-xm]
           adi.schema.meta [meta-schema meta-type-checks])

(defn ->datomic-schm [xm]
   (emit-dschm (:flat xm)))