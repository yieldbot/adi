(ns adi.emit.test-process
  (:use midje.sweet
        hara.common
        adi.emit.process
        adi.data.samples))

(fact "process-unnest-key"
  (process-unnest-key {:+ {:+ {:name 1}}})
  => {:name 1}

  (process-unnest-key {:+ {:name {:+ 1}}} :+)
  => {:name {:+ 1}}

  (process-unnest-key {:- {:- {:name 1}}} :-)
  => {:name 1})

(fact "process-make-key-tree"
  (process-make-key-tree {} {})
  => {}

  (process-make-key-tree {:name ""} {:name []})
  => {:name true}

  (process-make-key-tree {:a/b/c ""
                         :a/b/d ""
                         :a/b/e ""
                         :a/b/d/f/g ""}
                        {:a {:b {:c [] :d []}}})
  => {:a {:b {:c true :d true}}}

  (process-make-key-tree {:a {}} {:a {:b []}})
  => {:a {}}

  (process-make-key-tree {:account {:OTHER ""}}
                        {:account {:id [{:ident       :account/id
                                         :type        :long}]}})
  => {:account {}})

(fact "process-make-nss"
  (process-make-nss {} {})
  => #{}

  (process-make-nss {:name ""} {:name []})
  => #{}

  (process-make-nss {:a/b/c ""
                     :a/b/d ""
                     :a/b/e ""
                     :a/b/d/f/g ""}
                    {:a {:b {:c [] :d []}}})
  => #{:a/b}

  (process-make-nss {:a {}} {:a {:b []}})
  => #{:a}

  (process-make-nss {:account {:OTHER ""}}
                    {:account {:id [{:ident       :account/id
                                     :type        :long}]}})
  => #{:account})


(fact "process-assoc-keyword"
  (process-assoc-keyword {} {:type :keyword} :image/type :big)
  => {:image/type :big}

  (process-assoc-keyword {} {:type    :keyword
                             :keyword {:ns :image.type}} :image/type :big)
  => {:image/type :image.type/big})


(fact "process-init-assoc"
   (process-init-assoc {}
                      [{:ident :name :type :string}] "chris" {})
  => {:name "chris"}

  (process-init-assoc {:likes "ice-cream"}
                      [{:ident :name :type :string}] "chris" {})
  => {:name "chris"
      :likes "ice-cream"}

  (process-init-assoc {:likes "ice-cream"}
                      [{:ident       :name
                        :type        :string}]
                      #{"chris"} {})
  => (throws Exception) ;; Requires (:cardinality :many)

  (process-init-assoc {:likes "ice-cream"}
                      [{:ident       :name
                        :type        :string
                        :cardinality :many}]
                      #{"chris"} {})
  => {:name #{"chris"}
      :likes "ice-cream"}

  (process-init-assoc {} [{:ident :name
                           :type :keyword}]
                      :chris {})
  => {:name :chris}

  (process-init-assoc {} [{:ident :name
                           :type  :enum
                           :keyword  {:ns :name}
                           :cardinality :many}]
                      #{:adam :bob :chris} {})
  => {:name #{:name/adam :name/bob :name/chris}}

  (process-init-assoc {} [{:ident :name
                           :type  :enum
                            :enum  {:ns :name
                                    :values #{:chris}}}]
                      :chris {:options {:restrict? true}})
  => {:name :name/chris}

  (process-init-assoc {} [{:ident :name
                           :type  :enum
                            :enum  {:ns :name
                                    :values #{}}}]
                      :chris {:options {:restrict? true}})
  => (throws Exception))


(fact "process-init chain, single layer ref"
  (process-init-ref (-> s1-env :schema :geni :node :parent first)
                    {:value "hello"}
                    s1-env)
  => {:node/value "hello" :# {:nss #{:node}}}

  (process-init-assoc {}
                      (-> s1-env :schema :geni :node :parent)
                      {:value "hello"}
                      s1-env)
  => {:node/parent {:node/value "hello" :# {:nss #{:node}}}}

  (process-init {:node {:parent {:value "hello"}}}
                (-> s1-env :schema :geni)
                s1-env)
  => {:node/parent {:node/value "hello"
                  :# {:nss #{:node}}}
      :# {:nss #{:node}}}

  (process-init-ref (-> s1-env :schema :geni :node :children first)
                    {:value "hello"}
                    s1-env)
  => {:node/value "hello" :# {:nss #{:node}}}

  (process-init-assoc {}
                      (-> s1-env :schema :geni :node :children)
                      {:value "hello"}
                      s1-env)
  => {:node/children #{{:node/value "hello":# {:nss #{:node}}}}}

  (process-init {:node {:children {:value "hello"}}}
                (-> s1-env :schema :geni)
                s1-env)
  => {:node/children #{{:node/value "hello"
                        :# {:nss #{:node}}}}
      :# {:nss #{:node}}})

(fact
  (process-init-ref (-> s2-env :schema :geni :ns1 :next first)
                    {:next {}}
                    s2-env)
  => {:ns2/next {:# {:nss #{:ns1}}}
      :# {:nss #{:ns2}}}

  (process-init-ref (-> s2-env :schema :geni :ns1 :next first)
                    {:next {:next {}}}
                    s2-env)
  => {:# {:nss #{:ns2}}
      :ns2/next {:# {:nss #{:ns1}}, :ns1/next {:# {:nss #{:ns2}}}}}

  (process-init-ref (-> s2-env :schema :geni :ns1 :next first)
                    {:next {:prev {:next {:prev {:value "hello"}}}}}
                    s2-env)
  => {:# {:nss #{:ns2}},
      :ns2/next {:# {:nss #{:ns1}},
                 :ns1/prev
                 #{{:# {:nss #{:ns2}},
                    :ns2/next
                    {:# {:nss #{:ns1}},
                     :ns1/prev #{{:# {:nss #{:ns2}},
                                  :ns2/value "hello"}}}}}}})

(fact
  (process-init {:account {:id 1}}
                (-> s3-env :schema :geni)
                s3-env)
  => {:# {:nss #{:account}}, :account/id 1}

  (process-init {:account {:user {:id 1}}}
                (-> s3-env :schema :geni)
                s3-env)
  => {:# {:nss #{:account/user}}, :account/user/id 1}

  (process-init {:account {:id 1
                           :user {:id 1}}}
                (-> s3-env :schema :geni)
                s3-env)
  => {:# {:nss #{:account :account/user}}, :account/id 1, :account/user/id 1})



(fact "process-init-env"
  (process-init-env {} {})
  => (contains {:options {:defaults? true
                          :restrict? true
                          :required? true
                          :extras? false
                          :query? false
                          :sets-only? false}
                :schema  hash-map?})

  (process-init-env {} {:options {:defaults? false
                                  :restrict? false
                                  :required? false
                                  :extras? true
                                  :query? true
                                  :sets-only? true}})
  => (contains {:options {:defaults? false
                          :restrict? false
                          :required? false
                          :extras? true
                          :query? true
                          :sets-only? true}
                 :schema  hash-map?})

  (process-init-env {:name [{:type :string}]} {})
  => (contains {:options {:defaults? true
                          :extras? false
                          :required? true
                          :restrict? true
                          :query? false
                          :sets-only? false}
                :schema (contains {:fgeni {:name [{:cardinality :one
                                                   :ident :name
                                                   :type :string}]}
                                   :geni {:name [{:cardinality :one
                                                  :ident :name
                                                  :type :string}]}})}))

(fact "process-init"
  (let [pgeni {:name [{:type :string}]}]
    (process-init {:name "chris"} pgeni
                  (process-init-env pgeni {})))
  => {:# {:nss #{}}, nil "chris"}


  (let [pgeni {:name [{:ident       :name
                       :type        :string}]}]
    (process-init {:name "chris"} pgeni
                  (process-init-env pgeni {})))
  => {:# {:nss #{}}, :name "chris"}


  (let [pgeni {:account {:name [{:ident      :account/name
                                 :type       :string}]}}]
    (process-init {:account {:name "chris"}} pgeni
                  (process-init-env pgeni {})))
  => {:# {:nss #{:account}}, :account/name "chris"}


  (let [pgeni {:account {:name [{:ident       :account/name
                                 :type        :string
                                 :cardinality :many}]}}]
    (process-init {:account {:name "chris"}} pgeni

                  (process-init-env pgeni {})))
  => {:# {:nss #{:account}}, :account/name #{"chris"}}



  (let [pgeni {:account {:id [{:ident       :account/id
                               :type        :long
                               :restrict    odd?
                               :cardinality :many}]}}]
    (process-init {:account {:id 1}} pgeni
                  (process-init-env pgeni {:options {:restrict? true}})))
  => {:# {:nss #{:account}} :account/id #{1}}


  (let [pgeni {:account {:id [{:ident       :account/id
                               :type        :long
                               :restrict    odd?
                               :cardinality :many}]}}]
    (process-init {:account {:id 2}} pgeni
                  (process-init-env pgeni {:options {:restrict? true}})))
  => (throws Exception)

  (let [pgeni {:account {:id [{:ident       :account/id
                               :type        :long}]}}]
    (process-init {:account {}} pgeni
                  (process-init-env pgeni {})))
  => {:# {:nss #{:account}}}

  (process-make-key-tree {:account {}}
                         {:account {:id [{:ident       :account/id
                                          :type        :long}]}})

  (let [pgeni {:account {:id [{:ident       :account/id
                               :type        :long}]}}]
    (process-init {:account {:OTHER ""}} pgeni
                  (process-init-env pgeni {})))
  =>  (throws Exception)

  (let [pgeni {:account {:id [{:ident       :account/id
                               :type        :long}]}}]
    (process-init {:account {:OTHER ""}} pgeni
                  (process-init-env pgeni {:options {:extras? true}})))
  => {:# {:nss #{:account}}}


  (let [pgeni {:account {:id [{:ident       :account/id
                               :type        :long}]}}]
    (process-init {:account {:id 1}} pgeni
                  (process-init-env pgeni {:options {:sets-only? true}})))
  => {:# {:nss #{:account}} :account/id #{1}})


(fact "process-merge-defaults"
  (process-merge-defaults {} (-> s4-env :schema :fgeni)
                          #{:ns1/valB :ns1/valA :ns2/valA :ns2/valB}
                          s4-sgeni)
  => {:ns1/valA "A1" :ns1/valB "B1" :ns2/valB "B2"
      :ns2/valA "A2"}

  (process-merge-defaults {} (-> s4-env :schema :fgeni)
                          #{:ns1/valB :ns1/valA}
                          s4-sgeni)
  => {:ns1/valA "A1", :ns1/valB "B1"})


(fact "process-extras"
  (process-extras {:# {:nss #{:ns1 :ns2}}}
                  (-> s4-env :schema :fgeni)
                  {:label :default
                   :function process-merge-defaults}
                  s4-env)
  => (just {:ns1/valA "A1", :ns1/valB "B1",
            :ns2/valA "A2", :ns2/valB "B2"
            :# anything})

  (process-extras {:# {:nss #{:ns1}}}
                  (-> s4-env :schema :fgeni)
                  {:label :default
                   :function process-merge-defaults}
                  s4-env)
  => (just {:ns1/valA "A1", :ns1/valB "B1",
            :# anything})

  (process-extras {:# {:nss #{}}}
                  (-> s4-env :schema :fgeni)
                  {:label :default
                   :function process-merge-defaults}
                  s4-env)
  => {:# {:nss #{}}}

  (process-extras {:ns1/valA "stuff" :# {:nss #{:ns1}}}
                  (-> s4-env :schema :fgeni)
                  {:label :default
                   :function process-merge-defaults}
                  s4-env)
  => (just {:ns1/valB "B1", :ns1/valA "stuff", :# anything}))

(fact "process-extras"
  (process-extras {:# {:nss #{}}}
                  (-> s5-env :schema :fgeni)
                  {:label :default
                   :function process-merge-defaults}
                  s5-env)
  {:# {:nss #{}}}

  (process-extras {:# {:nss #{:nsA}}}
                  (-> s5-env :schema :fgeni)
                  {:label :default
                   :function process-merge-defaults}
                  s5-env)
  => {:nsA/val1 "A1", :nsA/val2 "A2", :# {:nss #{:nsA}}}

  (process-extras {:# {:nss #{:nsA/sub1}}}
                  (-> s5-env :schema :fgeni)
                  {:label :default
                   :function process-merge-defaults}
                  s5-env)
  => {:nsA/val1 "A1", :nsA/val2 "A2", :nsA/sub1/val "A_1" :# {:nss #{:nsA/sub1}}}

  (process-extras {:# {:nss #{:nsA :nsB}}}
                  (-> s5-env :schema :fgeni)
                  {:label :default
                   :function process-merge-defaults}
                  s5-env)
  => {:nsB/val2 "B2", :nsB/val1 "B1", :nsA/val1 "A1", :nsA/val2 "A2", :# {:nss #{:nsA :nsB}}})

(fact "process-extras-current"
  (-> (process-init {:node/children {:children {:children {}}}}
                    (-> s6-env :schema :geni)
                    s6-env)
      (process-extras (-> s6-env :schema :fgeni)
                      {:label :default
                       :function process-merge-defaults}
                      s6-env))
  => {:node/value "undefined", :# {:nss #{:node}},
      :node/children
      #{{:node/value "undefined", :# {:nss #{:node}},
         :node/children
         #{{:node/value "undefined", :# {:nss #{:node}},
            :node/children
            #{{:node/value "undefined", :# {:nss #{:node}}}}}}}}})

(fact "process-extras required"
  (process-extras {:account/id 0 :# {:nss #{:account}}}
                  (-> s7-env :schema :fgeni)
                  {:label :required
                   :function process-merge-required}
                  s7-env)
  => {:account/id 0, :# {:nss #{:account}}}

  (process-extras {:# {:nss #{:account}}}
                  (-> s7-env :schema :fgeni)
                  {:label :required
                   :function process-merge-required}
                  s7-env)
  => (throws Exception))

(fact "process"
  (process {:account/id 0}
           (process-init-env s7-sgeni {}))
  => {:# {:nss #{:account}}, :account/id 0}

  (process {:account/name "chris"}
           (process-init-env s7-sgeni {}))
  => (throws Exception)

   (process {:account/name "chris"}
           (process-init-env s7-sgeni {:options {:required? false}}))
   => {:# {:nss #{:account}}, :account/name "chris"})
