(ns adi.test-data
  (:use midje.sweet
        adi.utils
        adi.schema
        adi.data
        adi.checkers))

(fact "iid"
  (type (iid)) => (type #db/id[:db.part/user -100001])
  (iid -1) => #db/id[:db.part/user -1]
  (iid -1.3) => #db/id[:db.part/user -1]
  (iid 1) => #db/id[:db.part/user -1]
  (iid 1.3) => #db/id[:db.part/user -1]
  (iid "hello") => #db/id[:db.part/user -99162322]
  (iid \a) => #db/id[:db.part/user -97]
  (iid :hello) => #db/id[:db.part/user -1168742792])


(fact "adjust-safe-check"
  (adjust-safe-check (fn [x] (throw (Exception.))) :anything)
  => false

  (adjust-safe-check long? "1")
  => false

  (adjust-safe-check long? 2)
  => true

  (adjust-safe-check string? "1")
  => true

  (adjust-safe-check long? '_)
  => true

  (adjust-safe-check (fn [x] (throw (Exception.))) '_)
  => true)

(fact "adjust-value-sets-only"
  (adjust-value-sets-only #{} string? nil)
  => #{}

  (adjust-value-sets-only "1" string? nil)
  => #{"1"}

  (adjust-value-sets-only #{"1"} string? nil)
  => #{"1"}

  (adjust-value-sets-only #{"1" "2"} string? nil)
  => #{"1" "2"}

  (adjust-value-sets-only 1 string? nil)
  => (throws Exception)

  (adjust-value-sets-only #{1} string? nil)
  => (throws Exception)

  (adjust-value-sets-only #{1 "2"} string? nil)
  => (throws Exception))

(fact "adjust-value-normal"
  (adjust-value-normal "1" {} string? nil nil)
  => "1"

  (adjust-value-normal #{"1"} {} string? nil nil)
  => (throws Exception)

  (adjust-value-normal #{} {:cardinality :many} string? nil nil)
  => #{}

  (adjust-value-normal "1" {:cardinality :many} string? nil nil)
  => #{"1"}

  (adjust-value-normal #{"1" "2"} {:cardinality :many} string? nil nil)
  => #{"1" "2"}

  (adjust-value-normal "1" {} long? nil nil)
  => (throws Exception)

  (adjust-value-normal "1" {:cardinality :many} long? nil nil)
  => (throws Exception)

  (adjust-value-normal #{"1"} {:cardinality :many} long? nil nil)
  => (throws Exception))

(fact "adjust-value"
  (adjust-value "1" {} string? {} nil nil) => "1"

  (adjust-value "1" {} string?
                {:options {:sets-only? true}} nil nil)
  => #{"1"})

(fact "adjust-chk-type"
  (adjust-chk-type "1" {:type :string} {}) => "1"

  (adjust-chk-type "1" {:type :long} {}) => (throws Exception)

  (adjust-chk-type "1" {:type :string
                    :cardinality :many} {})
  => #{"1"}

  (adjust-chk-type "1" {:type :string}
               {:options {:sets-only? true}})
  => #{"1"})

(fact "adjust-chk-restrict"
  (adjust-chk-restrict 1 {:restrict odd?}
                   {:options {:restrict? true}})
  => 1

  (adjust-chk-restrict 2 {:restrict odd?}
                   {:options {:restrict? true}})
  => (throws Exception)

  (adjust-chk-restrict 2 {:restrict odd?} {})
  => 2

  (adjust-chk-restrict 2 {} {:options {:restrict? true}})
  => 2)

(fact "adjust use cases"
  (adjust "1" {:type :string} {})
  => "1"

  (adjust "1" {:type :long} {})
  => (throws Exception)

  (adjust 2 {:type :long
             :restrict? even?}
          {:options {:restrict? true}})
  => 2

  (adjust 2 {:type :long
             :restrict? even?}
          {:options {:restrict? true
                     :sets-only? true}})
  => #{2}


  (adjust #{2 4 6 8}
          {:type        :long
           :cardinality :many
           :restrict    even?}
          {:options {:restrict? true}})
  => #{2 4 6 8}

  (adjust 1
          {:type        :long
           :cardinality :many
           :restrict    even?}
          {:options {:restrict? true}})
  => (throws Exception)

  (adjust #{2 4 6 7}
          {:type        :long
           :cardinality :many
           :restrict    even?}
          {:options {:restrict? true}})
  => (throws Exception))


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
                           :enum  {:ns :name}
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


(def s1-sgeni {:node {:value  [{}]
                      :parent [{:type :ref
                                :ref  {:ns :node
                                       :rval :children}}]}})

(def s1-env (process-init-env s1-sgeni {}))

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

(def s2-sgeni {:ns1 {:value [{}]
                     :next  [{:type :ref
                              :ref  {:ns :ns2
                                     :rval :prev}}]}
               :ns2 {:value [{}]
                     :next  [{:type :ref
                              :ref  {:ns :ns1
                                     :rval :prev}}]}})

(def s2-env (process-init-env s2-sgeni {}))

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

(def s3-sgeni {:account {:id       [{:type :long}]
                         :business {:id   [{:type :long}]
                                    :name [{:type :string}]}
                         :user {:id    [{:type :long}]
                                :name  [{:type :string}]}}})

(def s3-env (process-init-env s3-sgeni {}))

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
                          :sets-only? false}
                :schema  hash-map?})

  (process-init-env {} {:options {:defaults? false
                                  :restrict? false
                                  :required? false
                                  :extras? true
                                  :sets-only? true}})
  => (contains {:options {:defaults? false
                          :restrict? false
                          :required? false
                          :extras? true
                          :sets-only? true}
                 :schema  hash-map?})

  (process-init-env {:name [{:type :string}]} {})
  => (contains {:options {:defaults? true
                          :extras? false
                          :required? true
                          :restrict? true
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


(def s4-sgeni {:ns1 {:valA  [{:default "A1"}]
                     :valB  [{:default (fn [] "B1")}]}
               :ns2 {:valA  [{:default "A2"}]
                     :valB  [{:default (fn [] "B2")}]}})

(def s4-env (process-init-env s4-sgeni {}))

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

(def s5-sgeni {:nsA {:sub1 {:val [{:default "A_1"}]}
                     :sub2 {:val [{:default "A_2"}]}
                     :val1  [{:default "A1"}]
                     :val2  [{:default "A2"}]}
               :nsB {:sub1 {:val [{:default "B_1"}]}
                     :sub2 {:val [{:default "B_2"}]}
                     :val1  [{:default "B1"}]
                     :val2  [{:default "B2"}]}})

(def s5-env (process-init-env s5-sgeni {}))

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


(def s6-sgeni {:node {:value  [{:default "undefined"}]
                      :parent [{:type :ref
                                :ref  {:ns :node
                                       :rval :children}}]}})

(def s6-env (process-init-env s6-sgeni {}))


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

(def s7-sgeni {:account {:id   [{:type     :long
                                 :required true}]
                         :name [{}]
                         :tags [{:cardinality :many}]}})

(def s7-env (process-init-env s7-sgeni {}))

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

(fact "characterise-nout"
  (characterise-nout :db {:id 0} s7-env {})
  => {:db {:id 0}}

  (characterise-nout :account/name "chris" s7-env {})
  => {:data-one {:account/name "chris"}}

  (characterise-nout :account/tags #{"t1"} s7-env {})
  {:data-many {:account/tags #{"t1"}}}

  (characterise-nout :node/parent {:node/value "parent1"} s6-env {})
  => {:refs-one {:node/parent {:data-one {:node/value "parent1"}}}}

  (characterise-nout :node/children #{{:node/value "child1"}} s6-env {})
  => {:refs-many {:node/_parent #{{:data-one {:node/value "child1"}}}}})

(fact "characterise"
  (characterise {:account/name "chris"} s7-env)
  => {:data-one {:account/name "chris"}}

  (characterise {:node/value "undefined"
                 :node/children #{}}
                s6-env)
  => {:refs-many {:node/_parent #{}}
      :data-one {:node/value "undefined"}}

  (characterise {:node/value "undefined"
                 :node/children #{{:node/value "child1"}}
                 :node/parent {:node/value "parent1"}}
                s6-env)

  => {:data-one {:node/value "undefined"}
      :refs-many {:node/_parent #{{:data-one {:node/value "child1"}}}}
      :refs-one {:node/parent {:data-one {:node/value "parent1"}}}}

  (characterise {:account/name #{"chris"}} s6-env)
  => (throws Exception)
)

(fact "characterise-gen-id"
  (characterise-gen-id {} {})
  => {}

  (characterise-gen-id {} {:generate {:ids {:function (constantly 1)}}})
  => {:db {:id 1}}

  (characterise-gen-id {:db {:id 3}} {:generate {:ids {:function (constantly 1)}}})
  => {:db {:id 3}})

(fact "characterise-gen-sym"
  (characterise-gen-sym {} {})
  => {}

  (characterise-gen-sym {} {:generate {:syms {:function (constantly '?e)}}})
  => {:# {:sym '?e}}

  (characterise-gen-sym {:# {:sym '?x}} {:generate {:syms {:function (constantly '?e)}}})
  => {:# {:sym '?x}})

(fact "characterise refs"
  (characterise
   (process {:ns1/prev {:value "hello"}} s2-env)
   (merge-in s2-env {:generate {:ids {:function (incremental-id-gen)}}}))
  =>  {:# {:nss #{:ns1}}, :db {:id 1}
       :refs-many
       {:ns2/_next
        #{{:# {:nss #{:ns2}}, :db {:id 2}
           :data-one {:ns2/value "hello"}}}}}

  (characterise
   (process {:ns1/next {:next {:value "hello"}}} s2-env)
   (merge-in s2-env {:generate {:ids {:function (incremental-id-gen)}}}))
  {, :# {:nss #{:ns1}}, :db {:id 1}
   :refs-one
   {:ns1/next
    {:refs-one {:ns2/next {:data-one {:ns1/value "hello"}, :# {:nss #{:ns1}}, :db {:id 3}}}, :# {:nss #{:ns2}}, :db {:id 2}}}}

  (characterise
   (process {:ns1/next {:next {:value "hello"}}} s2-env)
   (merge-in s2-env {:generate {:syms {:function (incremental-sym-gen 'n)}}}))
  => '{:# {:nss #{:ns1}, :sym ?n1}
       :refs-one
       {:ns1/next {:# {:nss #{:ns2}, :sym ?n2}
                   :refs-one
                   {:ns2/next {:# {:nss #{:ns1}, :sym ?n3}
                               :data-one {:ns1/value "hello"}}}}}})


(facts "datoms helper functions"
  (datoms-data-one {:db {:id 0}
                    :data-one {:account/name "chris"}})
  => [{:db/id 0, :account/name "chris"}]

  (datoms-data-many {:db {:id 0}
                     :data-many {:account/tags #{"t1" "t2" "t3"}}})
  => (just [[:db/add 0 :account/tags "t1"]
            [:db/add 0 :account/tags "t2"]
            [:db/add 0 :account/tags "t3"]] :in-any-order)

  (datoms-refs-one {:db {:id 0}
                    :refs-one {:node/parent {:db {:id 1}}}})
  => [[:db/add 0 :node/parent 1]]

  (datoms-refs-many {:db {:id 0}
                     :refs-many {:node/_parent #{{:db {:id 1}}
                                                 {:db {:id 2}}
                                                 {:db {:id 3}}}}})
  => (just [[:db/add 0 :node/_parent 1]
            [:db/add 0 :node/_parent 2]
            [:db/add 0 :node/_parent 3]] :in-any-order))


(facts "datoms"
  (datoms {:db {:id 0}
           :data-one {:node/value "undefined"}
           :refs-many {:node/_parent #{{:db {:id 1}
                                        :data-one {:node/value "child1"}}}}
           :refs-one {:node/parent {:db {:id 2}
                                    :data-one {:node/value "parent1"}}}})

  => (just [{:db/id 2, :node/value "parent1"}
            {:db/id 1, :node/value "child1"}
            {:db/id 0, :node/value "undefined"}
            [:db/add 0 :node/parent 2]
            [:db/add 0 :node/_parent 1]] :in-any-order))

(defn clause-env [env]
  (merge-in env
            {:options {:sets-only? true}
             :generate {:syms {:function (incremental-sym-gen 'e)
                        :not {:function (incremental-sym-gen 'n)}
                        :fulltext {:function (incremental-sym-gen 'ft)}}}}))

(facts "clauses-init"
  (clauses-data {:data-many {:account/name #{"adam" "bob" "chris"}}, :# {:sym '?e1}})
  => (just '[[?e1 :account/name "adam"]
             [?e1 :account/name "bob"]
             [?e1 :account/name "chris"]] :in-any-order)

  (clauses-refs (characterise {:node/children #{{}}
                               :node/parent #{{}}}
                              (clause-env s6-env)))
  => '[[?e1 :node/parent ?e3]
       [?e1 :node/_parent ?e2]]

  (clauses-init (characterise {:account/name #{"adam" "chris"}} (clause-env s7-env)))
  => '[[?e1 :account/name "adam"]
       [?e1 :account/name "chris"]]

  (clauses-init (characterise {:node/value #{"undefined"}
                               :node/children #{{:node/value #{"child1"}}}
                               :node/parent #{{:node/value #{"parent1"}}}}
                              (clause-env s6-env)))
  => '[[?e1 :node/value "undefined"]
       [?e1 :node/_parent ?e3]
       [?e1 :node/parent ?e2]
       [?e3 :node/value "child1"]
       [?e2 :node/value "parent1"]])

(facts "other clauses"
  (clauses-q {:# {:q '[?e :node/value "root"]}})
  => '[?e :node/value "root"]

  (clauses-not-gen
   (characterise {:node/value #{"undefined"}} (clause-env s6-env))
   '?x)
  => '[[?x :node/value ?e1]
       [(not= ?e1 "undefined")]]

  (-> (process {:node/value #{"undefined"}} (clause-env s6-env))
      (characterise (clause-env s6-env)))

  (clauses-not
   {:# {:sym '?x
        :not {:node/value #{"undefined"}}}}
   (clause-env s6-env))
  => '[[?x :node/value ?n1] [(not= ?n1 "undefined")]]

  (clauses-not
   {:# {:sym '?x
        :not {:node/value #{"undefined"}}}}
   (merge-in s6-env {:options {:sets-only? true}}))
  =>(just
     [(just ['?x :node/value anything])
      (just [(just ['not= anything "undefined"])])])

  (clauses-not-gen
   (characterise {:node/value #{"undefined" "root"}} (clause-env s6-env))
   '?x)
  => '[[?x :node/value ?e1]
       [(not= ?e1 "root")]
       [?x :node/value ?e1]
       [(not= ?e1 "undefined")]]

  (clauses-fulltext-gen
   (characterise {:node/value #{"undefined" "root"}} (clause-env s6-env))
   '?x)
  => '[[(fulltext $ :node/value "root") [[?x ?e1]]]
       [(fulltext $ :node/value "undefined") [[?x ?e1]]]]

  (clauses-fulltext
   {:# {:sym '?x
        :fulltext {:node/value #{"undefined"}}}}
   (clause-env s6-env))
  => '[[(fulltext $ :node/value "undefined") [[?x ?ft1]]]]

  (first (clauses-fulltext
           {:# {:sym '?x
                :fulltext {:node/value #{"undefined"}}}}
           (merge-in s6-env {:options {:sets-only? true}})))
  => (just ['(fulltext $ :node/value "undefined")
            (just [(just ['?x anything])])])

  (clauses {:node/next #{:node/value #{"undefined" "root"}}
            :# {:sym '?x
                :fulltext {:node/value #{"undefined"}}}}
           (clause-env s6-env))
  )
