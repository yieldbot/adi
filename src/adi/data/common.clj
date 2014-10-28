(ns adi.data.common
  (:require [datomic.api :as d]
            [clojure.edn :as edn]
            [hara.common.checks :refer [long?]]
            [ribol.core :refer [raise]]))

(defn iid
  "Constructs a new datomic db/id
  (iid 1) => #db/id[:db.part/user -1]

  (iid :hello) => #db/id[:db.part/user -245025397]"
  {:added "0.3"}
  ([] (d/tempid :db.part/user))
  ([obj]
     (let [v (if (number? obj) (long obj) (hash obj))
           v (if (< 0 v) (- v) v)]
       (d/tempid :db.part/user v ))))

(defn isym
  "Returns a new datomic symbol with a unique name. If a prefix string
  is supplied, the name is `prefix#` where `#` is some unique number. If
  prefix is not supplied, the prefix is `e_`.

  (isym) => => ?e_1238

  (isym \"v\") => => ?v1250
  "
  {:added "0.3"}
  ([] (isym 'e_))
 ([prefix] (symbol (str "?" (name (gensym prefix))))))

(defn vexpr->expr
 "checks whether an input is a vector expression
 (vecxpr->xpr [[\"_\"]]) => '_

 (vecxpr->xpr [[\"?hello\"]]) => '?hello

 (vecxpr->xpr [[\"(< ? 1)\"]]) => '(< ? 1)

 (vecxpr->xpr [[\":hello\"]]) => #db/id[:db.part/user -245025397]"
 {:added "0.3"}
 [v]
 (let [[[s]] v]
   (let [xpr (if (string? s) (edn/read-string s) s)]
     (cond
      (= '_ xpr) '_

      (list? xpr) xpr

      (long? xpr) xpr

      (and (symbol? xpr) (.startsWith s "?")) xpr

      (or (keyword? xpr)
          (symbol? xpr)) (iid xpr)

      :else
      (raise [:adi :wrong-input {:data v}]
             (str "VEXPR->EXPR: wrong input given in vector expression: " v))))))
