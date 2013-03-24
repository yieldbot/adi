(ns adi.data.test-process-required
  (:use adi.data
        adi.schema
        adi.utils
        midje.sweet))

(def pr1-geni
  (add-idents
   {:account  {:id        [{:type        :long
                            :required    true}]
               :name      [{:type        :string
                            :required    true}]
               :pass      [{:type        :string
                            :required    true}]
               :business  [{:type        :ref
                            :ref-ns      :business}]}
    :business {:id    [{:type        :long
                        :required    true}]
               :name  [{:type        :string
                        :required    true}]}}))

(def pr1-opts
  {:geni pr1-geni
   :fgeni (flatten-all-keys pr1-geni)
   :required? true})


(fact "process required"
  (process-required
   {:account/id   2
    :account/name "chris"
    :account/pass "hello"}
   pr1-opts)
  => {:account/id 2
      :account/name "chris"
      :account/pass "hello"}

  (process-required
   {:account/name "chris"
    :account/pass "hello"}
   pr1-opts)
  => (throws Exception)

  (process-required
   {:account/id 1
    :account/name "chris"
    :account/pass "hello"
    :account/business {}}
   pr1-opts)
  => (throws Exception)

  (process-required
   {:account/id 1
    :account/name "chris"
    :account/pass "hello"
    :account/business {:business/id   10
                       :business/name "cleanco"}}
   pr1-opts)
  => {:account/id 1
      :account/name "chris"
      :account/pass "hello"
      :account/business {:business/id   10
                         :business/name "cleanco"}}

    (process-required
   {:account/id 1
    :account/name "chris"
    :account/pass "hello"
    :account/business {:business/name "cleanco"}}
   pr1-opts)
  => (throws Exception))


(def pr4-geni
  (add-idents
   {:node  {:label     [{:type        :string}]
            :type      [{:type        :keyword
                         :keyword-ns  :value.type
                         :required    true
                         :cardinality :many
                         :default     :uncategorised}]
            :children  [{:type        :ref
                         :cardinality :many
                         :ref-ns      :node}]}}))

(def pr4-opts
  {:geni pr4-geni
   :fgeni (flatten-all-keys pr4-geni)
   :required? true})

(def pr4-idata
  (process-init
   {:node {:label "root"
           :type :root
           :children {:label "l1"
                      :type :l1
                      :children #{{:label "l2"
                                   :type :l2}}}}}
   pr4-geni
   pr4-opts))

(fact
  (process-required pr4-idata pr4-opts)
  => {:# {:nss #{:node}}
      :node/label "root"
      :node/type #{:value.type/root}
      :node/children
      #{{:# {:nss #{:node}}
         :node/label "l1"
         :node/type #{:value.type/l1}
         :node/children
         #{{:# {:nss #{:node}}
            :node/label "l2"
            :node/type #{:value.type/l2}}}}}}

  (process-required
   (process-init
    {:node {:label "root"
            :children {:label "l1"
                       :type :l1
                       :children #{{:label "l2"
                                    :type :l1}}}}}
    pr4-geni
    pr4-opts)
   pr4-opts)
  => (throws Exception)

  (process-required
   (process-init
    {:node {:label "root"
            :type :root
            :children {:label "l1"
                       :type :l1
                       :children #{{:label "l2"}}}}}
    pr4-geni
    pr4-opts)
   pr4-opts)
  => (throws Exception))
