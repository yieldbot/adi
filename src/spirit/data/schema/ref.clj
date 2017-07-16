(ns spirit.data.schema.ref
  (:require [spirit.data.schema.find :as find]
            [spirit.common.util.keys :as keys]
            [hara.string.path :as path]
            [hara.common.error :refer [error]]
            [inflections.core :as inf]))

(defn is-reversible?
  "determines whether a ref attribute is reversible or not
  (is-reversible? {:ident   :account/email    ;; okay
                   :type    :ref
                   :ref     {:ns  :email}})
  => true
  
  (is-reversible? {:ident   :email            ;; does not have keyword-ns
                   :type    :ref
                   :ref     {:ns  :email}})
  => false
  "
  {:added "0.3"}
  [attr]
  (if (and (= :ref (:type attr))
           (-> attr :ref :ns)
           (not (-> attr :ref :norev))
           (not (-> attr :ref :mutual))
           (path/path-ns (:ident attr))
           (-> attr :ident keys/keyword-reversed? not))
    true false))

(defn determine-rval
  "outputs the :rval value of a :ref schema reference

  (determine-rval [[:account :email false]
                   [{:ident  :account/email}]])
  => :accounts

  (determine-rval [[:account :email true]
                   [{:ident  :account/email}]])
  => :email_accounts

  (determine-rval [[:account :image true]
                   [{:ident  :account/bigImage}]])
  => :bigImage_accounts

  (determine-rval [[:node  :node  true]
                   [{:ident  :node/children
                     :ref    {:ns    :node}}]])
  => :children_of

  (determine-rval [[:node  :node  false]
                   [{:ident  :node/children
                     :ref    {:ns    :node
                              :rval  :parents}}]])
  => :parents
  "
  {:added "0.3"}
  [[[root ref-ns many?] [attr] :as entry]]
  (if-let [rval (-> attr :ref :rval)]
    rval
    (let [ident  (:ident attr)
          ival    (path/path-stem ident)]

      (cond (= root ref-ns)
            (let [rvec (concat (path/path-stem-vec ident) '("of"))]
              (path/join rvec "_"))

            many?
            (let [rvec (concat (path/path-stem-vec ident)
                               (list (->> root name inf/plural)))]
              (path/join rvec "_"))

            :else
            (->> root name inf/plural keyword)))))

(defn forward-ref-attr
  "creates the :ref schema attribute for the forward reference case

  (forward-ref-attr [{:ident  :node/children
                      :ref    {:ns    :node
                               :rval  :parents}}])
  => [{:ident    :node/children
       :ref      {:ns     :node
                  :type   :forward
                  :val    :children
                  :key    :node/children
                  :rval   :parents
                  :rkey   :node/_children
                  :rident :node/parents}}]

  (forward-ref-attr [{:ident  :node/children
                      :ref    {:ns    :node}}])
  => (throws Exception)"
  {:added "0.3"}
  [[attr]]
  (let [{:keys [ident ref]} attr
        {:keys [ns rval]}   ref]
    (if (and ident ref ns rval)
      [(update-in attr [:ref] merge
                  {:type    :forward
                   :key     ident
                   :val     (path/path-stem  ident)
                   :rkey    (keys/keyword-reverse ident)
                   :rident  (path/join [ns rval])})]
      (error "PREPARE_FORWARD_ATTR: Required keys: [ident, ref [ns rval]] " attr))))

(defn reverse-ref-attr
  "creates the reverse :ref schema attribute for backward reference

  (reverse-ref-attr [{:ident    :node/children
                      :ref      {:ns     :node
                                 :type   :forward
                                 :val    :children
                                 :key    :node/children
                                 :rval   :parents
                                 :rkey   :node/_children
                                 :rident :node/parents}}])
  => [{:ident :node/parents
       :cardinality :many
       :type :ref
       :ref  {:ns      :node
              :type    :reverse
              :val     :parents
              :key     :node/_children
              :rval    :children
              :rkey    :node/children
              :rident  :node/children}}]

  (reverse-ref-attr [{:ident    :node/children
                      :ref      {:ns     :node}}])
  => (throws Exception)"
  {:added "0.3"}
  [[attr]]
  (let [{:keys [ident ref]} attr
        {:keys [key val rkey rval rident]}  ref]
    (if (and ident ref key val rkey rval rident)
      [{:ident       rident
        :cardinality :many
        :type        :ref
        :ref         {:ns      (path/path-root ident)
                      :type    :reverse
                      :val     rval
                      :key     rkey
                      :rval    val
                      :rkey    key
                      :rident  ident}}]
      (error "PREPARE_REVERSE_ATTR: Required keys: [ident, ref [key val rkey rval rident]" attr))))

(defn forward-ref-attr-fn
  [[_ [attr] :as entry]]
  (forward-ref-attr
   [(assoc-in attr [:ref :rval] (determine-rval entry))]))

(defn attr-ns-pair
  "constructs a :ns and :ident root pair for comparison

  ;; (attr-ns-pair [{:ident  :a/b/c
  ;;                :ref     {:ns :d}}])
  ;; => [:a :d]

  (attr-ns-pair [{:ident  :a/b
                  :ref    {:ns :c}}])
  => [:a :c]"
  {:added "0.3"}
  [[attr]]
  (let [ident  (:ident attr)
        ref-ns (->  attr :ref :ns)]
    [(path/path-root ident) ref-ns]))

(defn mark-multiple
  "marks multiple ns/ident groups

  (mark-multiple [[[:a :b] [1 2]]
                  [[:c :d] [1]]])
  => [[[:c :d false] 1]
      [[:a :b true] 1] [[:a :b true] 2]]"
  {:added "0.3"}
  ([nsgroups] (mark-multiple nsgroups []))
  ([[nsgroup & more] output]
       (if-let [[nspair entries] nsgroup]
         (cond (< 1 (count entries))
               (recur more
                      (concat output
                              (map (fn [m] [(conj nspair true) m]) entries)))
               :else
               (recur more
                      (conj output [(conj nspair false) (first entries)])))
         output)))

(defn ref-attrs
  "creates forward and reverse attributes for a flattened schema

  (ref-attrs {:account/email [{:ident   :account/email
                               :type    :ref
                               :ref     {:ns  :email}}]})
  => {:email/accounts [{:ident :email/accounts
                        :cardinality :many
                        :type :ref
                        :ref {:type   :reverse
                              :key    :account/_email
                              :ns     :account
                              :val    :accounts
                              :rval   :email
                              :rident :account/email
                              :rkey   :account/email}}]
      :account/email  [{:ident :account/email
                        :type :ref
                        :ref {:type   :forward
                              :key    :account/email
                              :ns     :email
                              :val    :email
                              :rval   :accounts
                              :rident :email/accounts
                              :rkey   :account/_email}}]}"
  {:added "0.3"}
  [fschm]
  (let [refs  (vals (find/all-attrs fschm is-reversible?))
        lus   (group-by attr-ns-pair refs)
        fwds  (->> (seq lus)
                   (mark-multiple)
                   (map forward-ref-attr-fn))
        revs (->> fwds
                  (filter (fn [[attr]] (is-reversible? attr)))
                  (map reverse-ref-attr))
        all   (concat fwds revs)]
    (zipmap (map (fn [[attr]] (:ident attr)) all) all)))
