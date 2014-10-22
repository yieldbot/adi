(ns adi.data.normalise.common
  (:require [hara.common :refer [error suppress hash-map? hash-set? long? assoc-if
                                 keyword-join keyword-split
                                 keyword-ns keyword-ns? keyword-stem]]
            [hara.collection.hash-map :refer [treeify-keys-nested]]
            [adi.schema :refer [meta-type-checks]]
            [adi.data.coerce :refer [coerce]]
            [ribol.core :refer [raise]]))

(defn wrap-single-enums [f]
  (fn [subdata [attr] nsv interim fns env]
    (cond (= :enum (:type attr))
          (let [v (if (keyword-ns? subdata (-> attr :enum :ns))
                    (keyword-stem subdata)
                    subdata)
                chk (-> attr :enum :values)]
            (if-not (suppress (chk v))
              (raise [:adi :normalise :wrong-input 
                {:data subdata :nsv nsv :key-path (:key-path interim) :check chk}]
                (str "WRAP_SINGLE_ENUMS: " v " in " nsv " can only be one of: " chk))
              (f v [attr] nsv interim fns env)))
          :else
          (f subdata [attr] nsv interim fns env))))

(defn wrap-single-keywords [f]
  (fn [subdata [attr] nsv interim fns env]
    (cond (= :keyword (:type attr))
          (let [v (if (keyword-ns? subdata (-> attr :keyword :ns))
                    (keyword-stem subdata)
                    subdata)])
          :else
          (f subdata [attr] nsv interim fns env))))

(defn wrap-single-type-check [f]
  (fn [subdata [attr] nsv interim fns env]
    (let [t (:type attr)
          chk (meta-type-checks t)]
      (cond
       (chk subdata) (f subdata [attr] nsv interim fns env)

       (-> env :options :use-coerce)
       (f (coerce subdata t) [attr] nsv interim fns env)
       
       :else
       (raise [:adi :normalise :wrong-type 
               {:data subdata :nsv nsv :key-path (:key-path interim) :type t}]
               (str "WRAP_SINGLE_TYPE_CHECK: " subdata " in " nsv " is not of type " t))))))
