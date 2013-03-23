(ns adi.test-data
  (:use adi.data
        adi.schema
        adi.utils
        midje.sweet))

(fact "basic adjust"
  (adjust {:type :string} "chris" {})
  => "chris"

  (adjust {:type :string} '_ {})
  => '_

  (adjust {:type        :string
           :cardinality :many}
          "chris"
          {})
  => #{"chris"}
  (adjust {:type        :string
           :cardinality :many}
          #{"chris"}
          {})
  => #{"chris"}

  (adjust {:type        :string
           :cardinality :many}
          '_
          {})
  => #{'_}

  (adjust {:type        :string
           :cardinality :many}
          #{"chris" '_}
          {})
  => #{'_ "chris"})

(fact "adjust sets-only"
  (adjust {:type        :string}
          "chris"
          {:sets-only? true})
  => #{"chris"}

  (adjust {:type        :string}
          #{"chris"}
          {:sets-only? true})
  => #{"chris"})

(fact "adjust exceptions"
  (adjust {:type        :string} :keyword {})
  => (throws Exception)

    (adjust {:type        :string
             :cardinality :many}
            :one)
  => (throws Exception)

  (adjust {:type        :string
           :cardinality :many}
          #{:one :two})
  => (throws Exception))


(def f1-geni (add-idents {:name [{:type :string}]}))
(def f1-opts {:geni f1-geni} )

(fact "process-init on basic data"
  (process-init {} f1-geni f1-opts)
  => {}

  (process-init {:name "chris"}
                 f1-geni f1-opts)
  => {:name "chris"}

  (process-init {:other-value "something else"}
                 f1-geni f1-opts)
  => (throws Exception)

  (process-init {:other-value "something else"}
                f1-geni (assoc f1-opts :extras? true))
  => {}

  (process-init {:name :wrong-type}
                f1-geni f1-opts)
  => (throws Exception)

  (process-init {:+ {:name "chris"}}
                f1-geni f1-opts)
  => {:name "chris"}

  (process-init {:+ {:+ {:+ {:name "chris"}}}}
                f1-geni f1-opts)
  => {:name "chris"}

  (process-init {:name "chris"
                 :#/not     {:name "chris"}
                 :#/not-any [[:name "chris"]
                             [:name "adam"]] }
                f1-geni f1-opts)
  => {:name "chris"
      :# {:not {:name "chris"}
          :not-any [[:name "chris"]
                    [:name "adam"]]}})

(def f2-geni
  (add-idents
   {:account {:name  [{:type        :string}]
              :pass  [{:type        :string}]
              :tags  [{:type        :string
                       :cardinality :many}]}}))

(def f2-opts {:geni f2-geni})

(fact "process-init on layered data"
  (process-init {:account {:name "chris"
                           :pass "hello"
                           :tags #{"tag1" "tag2"}}}
                f2-geni
                f2-opts)
  => {:account/name "chris"
      :account/pass "hello"
      :account/tags #{"tag1" "tag2"}}

  (process-init {:account {:name "chris"
                           :pass "hello"}
                 :+ {:account {:tags #{"tag1" "tag2"}}}}
                f2-geni
                f2-opts)
  => {:account/name "chris"
      :account/pass "hello"
      :account/tags #{"tag1" "tag2"}}

  (process-init {:account {:name "chris"
                           :pass "hello"
                           :+ {:account {:tags #{"tag1" "tag2"}}}}}
                f2-geni
                f2-opts)
  => {:account/name "chris"
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
  => {:options :choice/one}

  (process-init {:options #{:one :two}}
                f3-geni
                (assoc f3-opts :sets-only? true))
  => {:options #{:choice/one :choice/two}}

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
  => {:node/value "root"
      :node/next  {:node/value "l1"
                   :node/next  {:node/value "l2"}}}

  (process-init {:node {:value "root"
                        :next  {:value "l1"
                                :next {:value "l2"}}}}
                f4-geni
                (assoc f4-opts :sets-only? true))
  => {:node/value #{"root"}
      :node/next  #{{:node/value #{"l1"}
                     :node/next  #{{:node/value #{"l2"}}}}}})


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
  => {:node/value "root"
      :node/children  #{{:node/value "l1a"}
                        {:node/value "l1b"}
                        {:node/value "l1c"}
                        {:node/value "l1d"}}}

  (process-init {:node {:value     "root"
                        :children  {:value "l1"
                                    :children   {:value "l2"}}}}
                f5-geni
                f5-opts)
  => {:node/value "root"
      :node/children  #{{:node/value "l1"
                         :node/children  #{{:node/value "l2"}}}}})
