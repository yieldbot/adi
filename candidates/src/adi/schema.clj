(ns adi.schema
  (:require [hara.data.path :as data]
            [adi.schema.alias :as alias]
            [adi.schema.find :as find]
            [adi.schema.ref :as ref]
            [adi.schema.meta :as meta]))

(defn simplify
  "helper function for easier display of adi schema
  (simplify {:account/name  [{:type :long}]
             :account/email [{:type :string
                              :cardinality :many}]
             :email/accounts [{:type :ref
                               :cardinality :many
                               :ref {:ns :account}}]})
  => {:email {:accounts :&account<*>}
      :account {:email :string<*> :name :long}}"
  {:added "0.3"}
  [flat]
  (->> flat
       (reduce-kv (fn [out k [attr]]
                    (let [card-str (condp = (:cardinality attr)
                                     :many "<*>"
                                     "")
                          type-str (condp = (:type attr)
                                     :ref (str "&" (name (-> attr :ref :ns)))
                                     (name (:type attr)))]
                      (assoc out k (keyword (str type-str card-str)))))
                  {})
       (data/treeify-keys)))

(defrecord Schema [flat tree lu]
  Object
  (toString [this]
    (str "#schema" (simplify flat))))

(defmethod print-method Schema
  [v w]
  (.write w (str v)))

(defn create-lookup
  "lookup from flat schema mainly for reverse refs
  (create-lookup
   {:account/name   [{}]
    :account/email  [{}]
    :email/accounts [{:ident :email/accounts
                      :type :ref
                      :ref {:type :reverse
                            :key :account/_email}}]})
  => {:email/accounts :email/accounts
      :account/_email :email/accounts
      :account/email :account/email
      :account/name :account/name}"
  {:added "0.3"}
  [fschm]
  (reduce-kv (fn [out k [attr]]
               (cond (find/is-reverse-ref? attr)
                     (assoc out (-> attr :ref :key) k k k)

                     :else
                     (assoc out k k)))
             {} fschm))

(defn create-flat-schema
  "creates a flat schema from an input map
  (create-flat-schema {:account {:email [{:type    :ref
                                          :ref     {:ns  :email}}]}})
  => {:email/accounts [{:ident :email/accounts
                        :type :ref
                        :cardinality :many
                        :ref {:ns :account
                              :type :reverse
                              :key :account/_email
                              :val :accounts
                              :rval :email
                              :rkey :account/email
                              :rident :account/email}}]
      :account/email [{:ident :account/email
                       :type :ref
                       :cardinality :one
                       :ref  {:ns :email
                              :type :forward
                              :key :account/email
                              :val :email
                              :rval :accounts
                              :rkey :account/_email
                              :rident :email/accounts}}]}"
  {:added "0.3"}
  [m]
  (let [fschm (->> (data/flatten-keys-nested m)
                   (map meta/attr-add-ident)
                   (map #(meta/attr-add-defaults % meta/all-auto-defaults))
                   (into {}))]
    (merge fschm
           (ref/ref-attrs fschm)
           (alias/alias-attrs fschm))))

(defn schema
  "creates an extended schema for use by adi
  (-> (schema {:account/name   [{}]
               :account/email  [{:ident   :account/email
                                 :type    :ref
                                 :ref     {:ns  :email}}]})
      :flat
      simplify)
  => {:email {:accounts :&account<*>}
      :account {:email :&email
                :name :string}}"
  {:added "0.3"}
  [m]
  (let [flat (create-flat-schema m)
        tree (data/treeify-keys flat)
        lu   (create-lookup flat)]
    (Schema. flat tree lu)))
