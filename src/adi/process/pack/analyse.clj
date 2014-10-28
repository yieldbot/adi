(ns adi.process.pack.analyse
  (:require [hara.common.checks :refer [hash-map? long?]]
            [hara.common.error :refer [error suppress]]
            [hara.string.path :as path]
            [hara.data.map :refer [assoc-if assoc-in-if assoc-nil]]
            [adi.schema.meta :as meta]
            [adi.data.checks :refer [db-id?]]
            [adi.data.common :refer [iid]]
            [adi.data.coerce :as coerce]
            [clojure.set :as set]
            [ribol.core :refer [raise]]))

(defn wrap-mutual-ref [f]
  (fn [v [attr] tsch fns]
    (let [v (f v [attr] tsch fns)
          id (:id fns)]
      (if (and (-> attr :ref :mutual)
               (= "datoms" (:type fns)))
        (cond (nil? id) v

              (hash-map? v)
              (assoc v (:ident attr) (:id fns))

              :else {:# {:id v} (:ident attr) (:id fns)})
        v))))

(defn analyse-attr-single-ref [v [attr] tsch fns]
 (cond (hash-map? v)
       (if-let [ns (-> attr :ref :ns)]
         ((:analyse fns) v tsch [ns] tsch fns)
         ((:analyse fns) v tsch [] tsch fns))

       (long? v)
       (cond (:ban-ids fns)
             (raise [:adi :analyse :id-banned
                     {:id v :attr attr}]
                    (str "ANALYSE_ATTR_SINGLE_REF: id " v " is not allowed for refs" ))

             (:ban-body-ids fns)
             (raise [:adi :analyse :body-id-banned
                     {:id v :attr attr}]
                    (str "ANALYSE_ATTR_SINGLE_REF: id " v " is not allowed for refs" ))

             :else v)

       (db-id? v) v

       (= v '_) v

       (symbol? v)
       (if (= (:type fns) "datoms")
         (iid v) v)

       :else
       (raise [:adi :analyse :invalid-input
               {:data v}]
              (str "ANALYSE_ATTR_SINGLE_REF: " v " is invalid for refs" ))))

(defn wrap-single-expressions [f]
  (fn [v [attr] tsch fns]
    (if (and (list? v) (:ban-expressions fns))
      (raise [:adi :analyse :expression-banned
              {:data v :attr attr}]
             (str "WRAP_SINGLE_EXPRESSIONS: " v " is banned because it is an expression.")))
    (f v [attr] tsch fns)))

(defn wrap-attr-restrict [f]
  (fn [v [attr] tsch fns]
    (if-let [chk (:restrict attr)]
      (let [[msg chk] (if (vector? chk) chk
                          ["" chk])]
        (if-not (suppress (chk v))
          (raise [:adi :analyse :failed-restriction
                  {:data v :attr attr :message msg}]
                 (str "WRAP_ATTR_RESTRICT: " v " fails restriction: " msg))
          (f v [attr] tsch fns)))
      (f v [attr] tsch fns))))

(defn wrap-attr-type-check [f]
  (fn [v [attr] tsch fns]
    (let [t (:type attr)
          chk (meta/type-checks t)
          nv  (cond (or (= v '_)
                        (symbol? v)
                        (list? v)
                        (chk v))
                    v
                    :else
                    (coerce/coerce v t))]
      (f nv [attr] tsch fns))))

(defn analyse-attr-single [v [attr] tsch fns]
 (if-let [t (:type attr)]
   (cond (= t :ref)
         ((wrap-mutual-ref analyse-attr-single-ref) v [attr] tsch fns)

         (= t :enum)
         (if-let [ens (-> attr :enum :ns)]
           (path/join [ens v])
           v)

         :else v)
   (error "ANALYSE-ATTR-SINGLE: Type data of " attr " missing")))

(defn analyse-attr [v [attr] tsch fns]
 (if (set? v)
   (set (map #((:analyse-single fns) % [attr] tsch fns) v))
   ((:analyse-single fns) v [attr] tsch fns)))

(defn analyse-loop
 ([tdata psch nsv tsch fns]
    (analyse-loop tdata psch nsv tsch fns {}))
 ([tdata psch nsv tsch fns output]
    ;;(println "ANALYSE_LOOP:" fns)
     (if-let [[k v] (first tdata)]
       (let [subsch (get-in psch (conj nsv k))]
         (cond (hash-map? subsch)
               (recur (next tdata) psch nsv tsch fns
                      (analyse-loop v psch (conj nsv k) tsch fns output))

               (vector? subsch)
               (recur (next tdata) psch nsv tsch fns
                      (assoc-if output (path/join (conj nsv k))
                                ((:analyse-attr fns) v subsch tsch fns)))

               (nil? subsch)
               (do (if-not (:schema-ignore fns)
                     (raise [:adi :analyse :no-schema {:nsv (conj nsv k) :data v}]
                            (str "ANALYSE_LOOP: " (conj nsv k) " has no schema.")))
                   (recur (next tdata) psch nsv tsch fns output))

               :else
               (do (if-not (:schema-ignore fns)
                       (raise [:adi :analyse :invalid-schema {:nsv (conj nsv k) :data v}]
                              (str "ANALYSE_LOOP: " (conj nsv k) " has invalid schema: " subsch)))
                   (recur (next tdata) psch nsv tsch fns output))))
       output)))

(defn wrap-id [f]
  (fn [tdata sch nsv tsch fns]
    ;;(println "WRAP_ID:" fns)
    (let [id (get-in tdata [:db :id])
          id (if (and (not id) (-> fns :auto-ids false? not))
               (gensym "?e")
               id)
         _  (when (long? id)
              (if (:ban-ids fns)
                (raise [:adi :normalise :id-banned
                        {:id id :data tdata :nsv nsv}]
                       (str "WRAP_ID: All ids are banned for " nsv)))
              (if (:in-body fns)
                (if (:ban-body-ids fns)
                  (raise [:adi :normalise :body-id-banned
                          {:id id :data tdata :nsv nsv}]
                         (str "WRAP_ID: All body ids are banned for " nsv)))
                (if (:ban-top-id fns)
                  (raise [:adi :normalise :top-id-banned
                          {:id id :data tdata}]
                         (str "WRAP_ID: Top level ids are banned.")))))
          ;;_     (println "FNS: " fns (assoc-nil fns :in-body true :id id))
          output (f (dissoc tdata :db) sch nsv tsch (assoc-nil fns :in-body true :id id))]
     (cond (:in-body fns)
           (cond (or (nil? id) (= id '_)) output

                 (and (symbol? id)
                      (= "query" (:type fns)))
                 (assoc-in-if output [:# :sym] id)

                 :else
                 (assoc-in-if output [:# :id] id))

           :else
           (cond (= "query" (:type fns))
                 (let [noutput (assoc-in output [:# :sym] '?self)]
                   (cond (or (nil? id) (= id '_)) noutput

                         :else (assoc-in-if noutput [:# :id] id)))

                 (or (nil? id) (= id '_)) output

                 :else
                 (assoc-in-if output [:# :id] id))))))

(defn wrap-plus [f]
  (fn [tdata psch nsv tsch fns]
    ;;(println "WRAP_PLUS:" fns)
   (let [tdata* (dissoc tdata :+)
         output (f tdata* psch nsv tsch fns)]
     (if-let [xdata (:+ tdata)]
       (merge ((:analyse fns) xdata tsch [] tsch fns) output)
       output))))

(defn analyse-nss [output]
 (let [pnss (or (get-in output [:# :nss]) #{})
       ks   (keys (dissoc output :#))
       nss  (disj (set (map path/path-ns ks)) nil)]
   (assoc-in output [:# :nss] (set/union pnss nss))))

(defn wrap-nss [f]
 (fn [tdata psch nsv tsch fns]
   (let [output (f tdata psch nsv tsch fns)
         output (analyse-nss output)]
     (if (empty? nsv) output
         (update-in output [:# :nss] #(set/union % (set nsv)))))))

(defn analyse
  "turns a nested-tree map into reference maps
  (analyse {:account {:name \"Chris\"}}
           {:schema (schema/schema examples/account-name-age-sex)
            :options {:auto-ids false}})
  => {:account/name \"Chris\"}

  (analyse {:account {:name :Chris
                      :age \"10\"}} ;; auto coercion
           {:schema (schema/schema examples/account-name-age-sex)
            :options {:auto-ids false}})
  => {:account/name \"Chris\", :account/age 10}
  "
  {:added "0.3"}
  [tdata env]
 (let [tsch (-> env :schema :tree)
       fns  {:analyse
             (let [f (-> analyse-loop
                         wrap-plus)
                   f (if (and (not= "query" (:type env))
                              (or (-> env :options :schema-defaults)
                                  (-> env :options :schema-required)))
                       (wrap-nss f) f)
                   f (wrap-id f)]
               f)

             :analyse-attr
             (let [f analyse-attr
                   f (if (-> env :options :schema-restrict) (wrap-attr-restrict f) f)]
               f)

             :analyse-single
             (let [f analyse-attr-single
                   f (wrap-single-expressions f)
                   f (if (-> env :options :skip-typecheck)
                       f (wrap-attr-type-check f))]
               f)

             :type            (-> env :type)
             :schema-ignore   (-> env :options :schema-ignore)
             :auto-ids        (-> env :options :auto-ids)
             :ban-ids         (-> env :options :ban-ids)
             :ban-top-id      (-> env :options :ban-top-id)
             :ban-body-ids    (-> env :options :ban-body-ids)
             :ban-expressions (-> env :options :ban-expressions)
             :ban-underscores (-> env :options :ban-underscores)}]
   ((:analyse fns) tdata tsch [] tsch fns)))
