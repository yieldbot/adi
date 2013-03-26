(ns adi.test-data
  (:use adi.data
        adi.schema
        adi.utils
        midje.sweet))

(fact "basic adjust"
  (adjust "chris" {:type :string} {})
  => "chris"

  (adjust '_ {:type :string} {})
  => '_

  (adjust "chris"
          {:type        :string
           :cardinality :many}
          {})
  => #{"chris"}
  (adjust #{"chris"}
          {:type        :string
           :cardinality :many}
          {})
  => #{"chris"}

  (adjust '_
          {:type        :string
           :cardinality :many}
          {})
  => #{'_}

  (adjust #{"chris" '_}
          {:type        :string
           :cardinality :many}
          {})
  => #{'_ "chris"})

(fact "adjust sets-only"
  (adjust "chris"
          {:type        :string}
          {:sets-only? true})
  => #{"chris"}

  (adjust #{"chris"}
          {:type        :string}
          {:sets-only? true})
  => #{"chris"})

(fact "adjust restriction"
  (adjust "chris"
          {:type        :string
           :restrict    #{"one"}}
          {:restrict? true})
  => (throws Exception)

  (adjust "chris"
          {:type        :string
           :restrict    #{"chris"}}
          {:restrict? true})
  => "chris"

  (adjust "chris"
          {:type        :string
           :cardinality :many
           :restrict    #{"chris"}}
          {:restrict? true})
  => #{"chris"}

  (adjust 2
          {:type        :long
           :cardinality :many
           :restrict    even?}
          {:restrict? true})
  => #{2}

  (adjust #{2 4 6 8}
          {:type        :long
           :cardinality :many
           :restrict    even?}
          {:restrict? true})
  => #{2 4 6 8}

  (adjust 1
          {:type        :long
           :cardinality :many
           :restrict    even?}
          {:restrict? true})
  => (throws Exception)

  (adjust #{2 4 6 7}
          {:type        :long
           :cardinality :many
           :restrict    even?}
          {:restrict? true})
  => (throws Exception)
  )

(fact "adjust exceptions"
  (adjust :keyword {:type        :string} {})
  => (throws Exception)

    (adjust :one
            {:type        :string
             :cardinality :many})
  => (throws Exception)

  (adjust #{:one :two}
          {:type        :string
           :cardinality :many})
  => (throws Exception))


(def f1-geni (add-idents {:name [{:type :string}]}))
(def f1-opts {:geni f1-geni} )

(fact "process-init on basic data"
  (process-init {} f1-geni f1-opts)
  => {:# {:nss #{}}}

  (process-init {:name "chris"}
                f1-geni f1-opts)
  => {:name "chris"
      :# {:nss #{:name}}}

  (process-init {:other-value "something else"}
                f1-geni f1-opts)
  => (throws Exception)

  (process-init {:other-value "something else"}
                f1-geni (assoc f1-opts :extras? true))
  => {:# {:nss #{:other-value}}}

  (process-init {:name :wrong-type}
                f1-geni f1-opts)
  => (throws Exception)

  (process-init {:+ {:name "chris"}}
                f1-geni f1-opts)
  => {:name "chris"
      :# {:nss #{:name}}}

  (process-init {:+ {:+ {:+ {:name "chris"}}}}
                f1-geni f1-opts)
  => {:name "chris"
      :# {:nss #{:name}}}

  (process-init {:name "chris"
                 :#/not     {:name "chris"}
                 :#/not-any [[:name "chris"]
                             [:name "adam"]] }
                f1-geni f1-opts)
  => {:name "chris"
      :# {:nss #{:name}
          :not {:name "chris"}
          :not-any [[:name "chris"]
                    [:name "adam"]]}})

(def f2-geni
  (add-idents
   {:account {:name  [{:type        :string}]
              :pass  [{:type        :string}]
              :tags  [{:type        :string
                       :cardinality :many}]}}))

(def f2-opts {:geni f2-geni})

(fact "process-init on various inputs"
  (process-init {:account {:name nil
                           :pass nil}}
                f2-geni f2-opts)
  => {:# {:nss #{:account}}}


  (process-init {:account {:name nil
                           :pass "hello"}}
                f2-geni f2-opts)
  => {:# {:nss #{:account}}
      :account/pass "hello"}

  (process-init {:account {:name "chris"
                           :pass "hello"}}
                f2-geni f2-opts)
  => {:# {:nss #{:account}}
      :account/name "chris" :account/pass "hello"}

  (process-init {:account/name "chris"
                 :account {:pass "hello"}}
                f2-geni f2-opts)
  => {:# {:nss #{:account}}
      :account/name "chris" :account/pass "hello"}

  (process-init {:+ {:account/name "chris"}
                 :account {:pass "hello"}}
                f2-geni f2-opts)
  => {:# {:nss #{:account}}
      :account/name "chris" :account/pass "hello"}

  (process-init {:+ {:account {:name "chris"}}
                 :account/pass "hello"}
                f2-geni f2-opts)
  => {:# {:nss #{:account}}
      :account/name "chris" :account/pass "hello"}

  (process-init {:+ {:account/name "chris"}
                 :account {:+ {:account {:pass "hello"}}}}
                f2-geni f2-opts)
  => {:# {:nss #{:account}}
      :account/name "chris" :account/pass "hello"}

  (process-init {:+ {:account/name "chris"
                     :+ {:+ {:account {:pass "hello"}}}}}
                f2-geni f2-opts)
  => {:# {:nss #{:account}}
      :account/name "chris" :account/pass "hello"}


  (process-init {:account/tags #{"fun" "happy" "still"}} f2-geni f2-opts)
  => {:# {:nss #{:account}}
      :account/tags #{"fun" "happy" "still"}}
  )

(fact "process-init on layered data"
  (process-init {:account {:name "chris"
                           :pass "hello"
                           :tags #{"tag1" "tag2"}}}
                f2-geni
                f2-opts)
  => {:# {:nss #{:account}}
      :account/name "chris"
      :account/pass "hello"
      :account/tags #{"tag1" "tag2"}}

  (process-init {:account {:name "chris"
                           :pass "hello"}
                 :+ {:account {:tags #{"tag1" "tag2"}}}}
                f2-geni
                f2-opts)
  => {:# {:nss #{:account}}
      :account/name "chris"
      :account/pass "hello"
      :account/tags #{"tag1" "tag2"}}

  (process-init {:account {:name "chris"
                           :pass "hello"
                           :+ {:account {:tags #{"tag1" "tag2"}}}}}
                f2-geni
                f2-opts)
  => {:# {:nss #{:account}}
      :account/name "chris"
      :account/pass "hello"
      :account/tags #{"tag1" "tag2"}})


(def f3-geni
  (add-idents
   {:options [{:type       :keyword
               :keyword-ns :choice}]}))

(def f3-opts {:geni f3-geni})

(fact "process-init on keywords"
  (process-init {:options :one}
                f3-geni
                f3-opts)
  => {:# {:nss #{:options}}
      :options :choice/one}

  (process-init {:options #{:one :two}}
                f3-geni
                (assoc f3-opts :sets-only? true))
  => {:# {:nss #{:options}}
      :options #{:choice/one :choice/two}}

  (process-init {:options #{:one :two}}
                f3-geni
                f3-opts)
  => (throws Exception))

(def f4-geni
  (add-idents
   {:node {:value [{:type    :string}]
           :next  [{:type    :ref
                    :ref-ns  :node}]}}))

(def f4-opts {:geni f4-geni})

(fact "process-init with ref"
  (process-init {:node {:value "root"
                        :next  {:value "l1"
                                :next {:value "l2"}}}}
                f4-geni
                f4-opts)
  => {:# {:nss #{:node}}
      :node/value "root"
      :node/next  {:# {:nss #{:node}}
                   :node/value "l1"
                   :node/next  {:# {:nss #{:node}}
                                :node/value "l2"}}}

  (process-init {:node {:value "root"
                        :next  {:value "l1"
                                :next {:value "l2"}}}}
                f4-geni
                (assoc f4-opts :sets-only? true))
  => {:# {:nss #{:node}}
      :node/value #{"root"}
      :node/next  #{{:# {:nss #{:node}}
                     :node/value #{"l1"}
                     :node/next  #{{:# {:nss #{:node}}
                                    :node/value #{"l2"}}}}}})


(def f5-geni
  (add-idents
   {:node {:value     [{:type        :string}]
           :children  [{:type        :ref
                        :ref-ns      :node
                        :cardinality :many}]}}))

(def f5-opts {:geni f5-geni})

(fact "process-init with ref"
  (process-init {:node {:value     "root"
                        :children  #{{:value "l1a"}
                                     {:value "l1b"}
                                     {:value "l1c"}
                                     {:value "l1d"}}}}
                f5-geni
                f5-opts)
  => {:# {:nss #{:node}}
      :node/value "root"
      :node/children  #{{:# {:nss #{:node}}
                         :node/value "l1a"}
                        {:# {:nss #{:node}}
                         :node/value "l1b"}
                        {:# {:nss #{:node}}
                         :node/value "l1c"}
                        {:# {:nss #{:node}}
                         :node/value "l1d"}}}

  (process-init {:node {:value     "root"
                        :children  {:value "l1"
                                    :children   {:value "l2"}}}}
                f5-geni
                f5-opts)
  => {:# {:nss #{:node}}
      :node/value "root"
      :node/children  #{{:# {:nss #{:node}}
                         :node/value "l1"
                         :node/children  #{{:# {:nss #{:node}}
                                            :node/value "l2"}}}}})
