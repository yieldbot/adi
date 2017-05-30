(ns spirit.datomic.process.normalise.base
  (:require [hara.common.checks :refer [hash-map? long?]]
            [hara.data.path :as data]
            [hara.string.path :as path]
            [hara.data.map :refer [assoc-if]]
            [spirit.datomic.data.checks :as checks]
            [hara.event :refer [raise]]))

(def tree-directives
  #{:pre-require       ;;
    :pre-mask          ;;
    :pre-transform     ;;
    :fill-assoc        ;;
    :fill-empty        ;;
    :ignore            ;;
    :allow             ;;
    :expression        ;;
    :validate          ;;
    :convert           ;;
    :post-transform    ;;
    :post-mask         ;;
    :post-require      ;;
    })

(defn submaps
  "creates a submap based upon a lookup subkey
  (submaps {:allow  {:account :check}
            :ignore {:account :check}} #{:allow :ignore} :account)
  => {:allow :check, :ignore :check}"
  {:added "0.3"}
  [m options subk]
  (reduce (fn [out option]
            (let [sv (get-in m [option subk])]
              (assoc out option sv)))
          (apply dissoc m (seq options))
          options))

(defn normalise-loop [tdata tsch nsv interim fns datasource]
  (reduce-kv (fn [output k subdata]
               (let [subsch (get tsch k)
                     pinterim (submaps interim tree-directives k)]
                (cond (nil? subsch)
                      (assoc-if output k
                                ((:normalise-nil fns)
                                 subdata nil (conj nsv k) pinterim datasource))

                      (hash-map? subsch)
                      (assoc-if output k
                                ((:normalise-branch fns)
                                 subdata subsch (conj nsv k) pinterim fns datasource))

                      (vector? subsch)
                      (assoc-if output k
                                ((:normalise-attr fns)
                                 subdata subsch (conj nsv k) pinterim fns datasource))
                      :else
                      (let [nnsv (conj nsv k)]
                        (raise [:normalise :wrong-input {:data subdata :nsv nnsv :key-path (:key-path interim)}]
                          (str "NORMALISE_LOOP: In " nsv ", " subdata " needs to be a vector or hashmap.")))
                      )))
             {} tdata))

(defn normalise-nil [subdata _ nsv interim datasource]
  (raise [:normalise :no-schema {:nsv nsv :key-path (:key-path interim) :ref-path (:ref-path interim)}]
         (str "NORMALISE_NIL: " nsv " is not in the schema.")))

(defn normalise-attr [subdata [attr] nsv interim fns datasource]
  (cond (set? subdata)
        (-> (map #((:normalise-single fns) % [attr] nsv interim fns datasource) subdata)
            (set)
            (disj nil))

        :else
        ((:normalise-single fns) subdata [attr] nsv interim fns datasource)))

(defn normalise-single [subdata [attr] nsv interim fns datasource]
  (if (= (:type attr) :ref)
    (cond (hash-map? subdata)
          (let [nnsv (path/split (-> attr :ref :ns))]
            ((:normalise fns)
             (data/treeify-keys-nested subdata)
             (get-in datasource (concat [:schema :tree] nnsv))
             nnsv interim fns datasource))

          (or (long? subdata) (checks/db-id? subdata))
          subdata

          :else
          (raise [:normalise :wrong-input {:nsv nsv :key-path (:key-path interim)}]
            (str "NORMALISE_SINGLE: In " nsv "," subdata " should be either a hashmaps or ids, not ")))
    subdata))

(defn normalise-expression [subdata [attr] nsv interim datasource] subdata)

(defn normalise-wrap [fns wrappers]
  (reduce-kv (fn [out k f]
               (let [nf (if-let [wrapvec (get wrappers k)]
                          (reduce (fn [f wrapper] (wrapper f)) f wrapvec)
                          f)]
                 (assoc out k nf)))
             {} fns))

(defn normalise
  "base normalise function for testing purposes

  (normalise {:account/name \"Chris\"
              :account/age 10}
             {:schema (schema/schema examples/account-name-age-sex)})
  => {:account {:age 10, :name \"Chris\"}}

  (normalise {:link/value \"hello\"
              :link {:next/value \"world\"
                     :next/next {:value \"!\"}}}
             {:schema (schema/schema examples/link-value-next)})
  => {:link {:next {:next {:value \"!\"}
                    :value \"world\"}
             :value \"hello\"}}"
  {:added "0.3"}
  ([data datasource & [wrappers]]
   (let [tdata (data/treeify-keys-nested data)
         tsch (-> datasource :schema :tree)
         interim (:pipeline datasource)
         fns {:normalise normalise-loop
              :normalise-nil normalise-nil
              :normalise-branch normalise-loop
              :normalise-attr normalise-attr
              :normalise-expression normalise-expression
              :normalise-single normalise-single}
         fns (normalise-wrap fns wrappers)]
     ((:normalise fns) tdata tsch [] interim fns datasource))))
