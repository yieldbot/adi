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

(fact "process-present-tree"
  (process-present-tree {} {})
  => {}

  (process-present-tree {:name ""} {:name []})
  => {:name true}

  (process-present-tree {:a/b/c ""
                         :a/b/d ""
                         :a/b/e ""
                         :a/b/d/f/g ""}
                        {:a {:b {:c [] :d []}}})
  => {:a {:b {:c true :d true}}}

  (process-present-tree {:a {}} {:a {:b []}})
  => {:a true}

  (process-present-tree {:account {:OTHER ""}}
                        {:account {:id [{:ident       :account/id
                                         :type        :long}]}})
  => {:account true})


(fact "process-keyword-assoc"
  (process-keyword-assoc {} {} :image/type :big)
  => {:image/type :big}

  (process-keyword-assoc {} {:keyword-ns :image.type} :image/type :big)
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
  => (throws Exception)

  (process-init-assoc {:likes "ice-cream"}
                      [{:ident       :name
                        :type        :string
                        :cardinality :many}]
                      #{"chris"} {})
  => {:name #{"chris"}
      :likes "ice-cream"})

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
