(ns spirit.core.datomic.core.model
  (:require [hara.common
             [error :refer [error]]
             [checks :refer [hash-map?]]]
            [hara.data.nested :refer [merge-nested]]
            [hara.string.path :as path]))

(declare model-input)

(defn model-input-branch-directive
  [[attr] directive]
  (if (= :ref (:type attr))
    :unchecked
    directive))

(defn model-input-branch-schema
  ([subsch directive]
     (model-input-branch-schema subsch directive {}))
  ([subsch directive output]
     (if-let [[k v] (first subsch)]
       (cond (hash-map? v)
             (recur (next subsch) directive
                    (assoc output k (model-input-branch-schema v directive)))
             (vector? v)
             (recur (next subsch) directive
                    (assoc output k (model-input-branch-directive v directive)))
             :else
             (error "MODEL_INPUT_BRANCH_SCHEMA: " v " for key "
                    k " of " subsch " should be a hashmap (branch) or vector (attr)"))
       output)))

(defn model-input-branch [v dft subsch tsch]
  (cond (hash-map? v)
        (merge-nested
         (model-input-branch-schema subsch dft)
         (model-input v dft subsch tsch {}))

        (or (= v :checked) (= v :unchecked))
        (model-input-branch-schema subsch v)

        :else
        (error "MODEL_INPUT_BRANCH: Branches only take :checked and :unchecked as directives")))

(defn model-input-attr-ref [v dft [attr] tsch]
  (cond (or (= v :unchecked) (= v :id) (= v :yield))
        v

        (= v :checked)
        (let [nsk (-> attr :ref :ns)
              subsch (if nsk (get tsch nsk) tsch)]
          (model-input-branch-schema subsch :checked))

        (hash-map? v)
        (let [nsk (-> attr :ref :ns)
              subsch (if nsk (get tsch nsk) tsch)]
          (model-input-branch v dft subsch tsch))

        :else
        (error "MODEL_INPUT_ATTR_REF: Values only take :checked, :unchecked, :id and :yield as directives")))

(defn model-input-attr [v dft [attr] tsch]
  (if (= :ref (:type attr))
    (model-input-attr-ref v dft [attr] tsch)
    (if (or (= v :checked) (= v :unchecked))
      v
      (error "MODEL_INPUT_ATTR: Values only take :checked and :unchecked as directives"))))

(defn model-input
  ([tmodel dft tsch]
     (model-input tmodel dft tsch tsch {}))
  ([tmodel dft psch tsch output]
     (if-let [[k v] (first tmodel)]
       (let [subsch (get psch k)]
         (cond (hash-map? subsch)
               (recur (next tmodel) dft psch tsch
                      (assoc output k (model-input-branch v dft subsch tsch)))

               (vector? subsch)
               (recur (next tmodel) dft psch tsch
                      (assoc output k (model-input-attr v dft subsch tsch)))

               :else
               (error "MODEL_INPUT: " v " for key " k " should be a hashmap (branch) or vector (attr)")))
       output)))

(declare model-unpack)

(defn model-unpack-branch [k v sch nsv tsch output]
 (if-let [subsch (get sch k)]
   (cond (hash-map? subsch)
         (model-unpack v subsch (conj nsv k) tsch output)

         (vector? subsch)
         (let [[attr] subsch]
           (if (= :ref (:type attr))
             (let [nnsv (path/split (-> attr :ref :ns))]
               (assoc output (path/join (conj nsv k))
                      (model-unpack v (get-in tsch nnsv) nnsv tsch {})))
             (error "MODEL_RETURN_BRANCH: Attribute can only be a ref" )))

         :else
         (error "MODEL_RETURN_BRANCH: subsch - " v " - has to be a hashmap or vector"))

   (error "MODEL_RETURN_BRANCH: Schema does not exist for " (conj nsv k))))

(defn model-unpack-attr [k v sch nsv output]
 (if-let [subsch (get sch k)]
   (let [[attr] subsch]
     (cond (and (= :ref (:type attr))
                (= :checked v))
           (error "MODEL_RETURN_ATTR: Allow cannot be a directive for a ref")

           :else
           (assoc output (path/join (conj nsv k)) v)))
   (error "MODEL_RETURN_ATTR: Schema does not exist for " (conj nsv k) sch)))

(defn model-unpack
 ([tmodel tsch]
    (model-unpack tmodel tsch [] tsch {}))
 ([tmodel sch nsv tsch output]
    (if-let [[k v] (first tmodel)]
      (recur (next tmodel) sch nsv tsch
             (cond (= :+ k)
                   (merge (model-unpack v tsch) output)

                   (hash-map? v)
                   (model-unpack-branch k v sch nsv tsch output)

                   (keyword? v)
                   (model-unpack-attr k v sch nsv output)

                   :else
                   (error "MODEL_RETURN: v - " v " - has to be a hashmap, vector or keyword")))
      output)))
