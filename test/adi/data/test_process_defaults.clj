(ns adi.data.test-process-defaults
  (:use adi.data
        adi.schema
        adi.utils
        midje.sweet))

(def pd1-geni
  (add-idents
   {:account  {:id        [{:type        :long
                            :default     (fn [] 1)}]
               :name      [{:type        :string}]
               :pass      [{:type        :string}]
               :business  [{:type        :ref
                            :ref-ns      :business}]}
    :business {:id    [{:type        :long
                        :default     (fn [] 10)}]
               :name  [{:type        :string}]}}))

(def pd1-opts
  {:geni pd1-geni
   :fgeni (flatten-keys-in pd1-geni)
   :defaults? true})

(fact "process defaults"
  (process-defaults
   {:account/id   2
    :account/name "chris"
    :account/pass "hello"}
   pd1-opts)
  => {:account/id 2
      :account/name "chris"
      :account/pass "hello"}

  (process-defaults
   {:account/name "chris"
    :account/pass "hello"}
   pd1-opts)
  => {:account/id 1
      :account/name "chris"
      :account/pass "hello"}

  (process-defaults
   {:account/name "chris"
    :account/pass "hello"
    :account/business {}}
   pd1-opts)
  => {:account/id 1
      :account/name "chris"
      :account/pass "hello"
      :account/business {:business/id  10}}

  (process-defaults
   {:account/name "chris"
    :account/pass "hello"
    :account/business {:business/name "cleanco"}}
   pd1-opts)
  => {:account/id 1
      :account/name "chris"
      :account/pass "hello"
      :account/business {:business/id   10
                         :business/name "cleanco"}})


(def pd2-geni
  (add-idents
   {:link  {:label     [{:type        :string}]
            :value     [{:type        :keyword
                         :keyword-ns  :value.type
                         :default     :uncategorised}]
            :next      [{:type        :ref
                         :ref-ns      :link}]}}))

(def pd2-opts
  {:geni pd2-geni
   :fgeni (flatten-keys-in pd2-geni)
   :defaults? true})

(def pd2-idata
  (process-init
   {:link {:label "root"
           :next {:label "l1"
                  :next {:label "l2"
                         :next {:label "l3"}}}}}
   pd2-geni
   pd2-opts))

(fact
  (process-defaults pd2-idata pd2-opts)
  => {:# {:nss #{:link}}
      :link/label "root"
      :link/value :value.type/uncategorised
      :link/next
      {:# {:nss #{:link}}
       :link/label "l1"
       :link/value :value.type/uncategorised
       :link/next
       {:# {:nss #{:link}}
        :link/label "l2"
        :link/value :value.type/uncategorised
        :link/next
        {:# {:nss #{:link}}
         :link/label "l3"
         :link/value :value.type/uncategorised}}}})



(def pd3-geni
  (add-idents
   {:node  {:label     [{:type        :string}]
            :value     [{:type        :keyword
                         :keyword-ns  :value.type
                         :default     :uncategorised}]
            :children  [{:type        :ref
                         :cardinality :many
                         :ref-ns      :node}]}}))

(def pd3-opts
  {:geni pd3-geni
   :fgeni (flatten-keys-in pd3-geni)
   :defaults? true})

(def pd3-idata
  (process-init
   {:node {:label "root"
           :children {:label "l1"
                      :children #{{:label "l2a"}
                                  {:label "l2b"}
                                  {:label "l2c"}}}}}
   pd3-geni
   pd3-opts))

(fact
  (process-defaults pd3-idata pd3-opts)
  => {:# {:nss #{:node}}
      :node/label "root"
      :node/value :value.type/uncategorised
      :node/children
      #{{:# {:nss #{:node}}
         :node/label "l1"
         :node/value :value.type/uncategorised
         :node/children
         #{{:# {:nss #{:node}}
            :node/label "l2a"
            :node/value :value.type/uncategorised}
           {:# {:nss #{:node}}
            :node/label "l2b"
            :node/value :value.type/uncategorised}
           {:# {:nss #{:node}}
            :node/label "l2c"
            :node/value :value.type/uncategorised}}}}})

(def pd4-geni
  (add-idents
   {:node  {:label     [{:type        :string}]
            :type      [{:type        :keyword
                         :keyword-ns  :value.type
                         :cardinality :many
                         :default     :uncategorised}]
            :children  [{:type        :ref
                         :cardinality :many
                         :ref-ns      :node}]}}))

(def pd4-opts
  {:geni pd4-geni
   :fgeni (flatten-keys-in pd4-geni)
   :defaults? true})

(def pd4-idata
  (process-init
   {:node {:label "root"
           :type :root
           :children {:label "l1"
                      :type :l1
                      :children #{{:label "l2a"}
                                  {:label "l2b"
                                   :type :l2}
                                  {:label "l2c"}}}}}
   pd4-geni
   pd4-opts))

(fact
  (process-defaults pd4-idata pd4-opts)
  => {:# {:nss #{:node}}
      :node/label "root"
      :node/type #{:value.type/root}
      :node/children
      #{{:# {:nss #{:node}}
         :node/label "l1"
         :node/type #{:value.type/l1}
         :node/children
         #{{:# {:nss #{:node}}
            :node/label "l2a"
            :node/type #{:value.type/uncategorised}}
           {:# {:nss #{:node}}
            :node/label "l2b"
            :node/type #{:value.type/l2}}
           {:# {:nss #{:node}}
            :node/label "l2c"
            :node/type #{:value.type/uncategorised}}}}}})
