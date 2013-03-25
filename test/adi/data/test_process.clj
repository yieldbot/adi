(ns adi.data.test-process
  (:use adi.data
        adi.schema
        adi.utils
        midje.sweet))

(def p1-geni
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


(fact "process defaults"
  (process {:account {:id   2
                      :name "chris"
                      :pass "hello"}}
           p1-geni)
  => {:account/id 2
      :account/name "chris"
      :account/pass "hello"
      :# {:nss #{:account}}}

  (process {:account {:name "chris"
                      :pass "hello"}}
           p1-geni)
  => {:account/id 1
      :account/name "chris"
      :account/pass "hello"
      :# {:nss #{:account}}}

  (process {:account {:name "chris"
                      :pass "hello"
                      :business {}}}
           p1-geni)
  => {:# {:nss #{:account}}
      :account/id 1
      :account/name "chris"
      :account/pass "hello"
      :account/business {:# {:nss #{:business}}
                         :business/id  10}}

  (process {:account {:name "chris"
                      :pass "hello"
                      :business {:name "cleanco"}}}
           p1-geni)
  => {:# {:nss #{:account}}
      :account/id 1
      :account/name "chris"
      :account/pass "hello"
      :account/business {:# {:nss #{:business}}
                         :business/id   10
                         :business/name "cleanco"}})

(def p2-geni
  (add-idents
   {:account  {:name      [{:type        :string
                            :required    true}]
               :pass      [{:type        :string}]
               :business  [{:type        :ref
                            :ref-ns      :business}]}
    :business {:name      [{:type        :string
                            :required    true}]}}))


(fact
  (process {:account {:name "chris"
                      :pass "hello"}}
           p2-geni
           {:required? true})
  => {:# {:nss #{:account}}
      :account/name "chris"
      :account/pass "hello"}

  (process {:account {:pass "hello"}}
           p2-geni
           {:required? true})
  => (throws Exception)

  (process {:account {:name "chris"
                      :pass "hello"
                      :business {}}}
           p2-geni
           {:required? true})
  => (throws Exception)

  (process {:account {:name "chris"
                      :pass "hello"
                      :business {:name "cleanco"}}}
           p2-geni
           {:required? true})
  => {:# {:nss #{:account}}
      :account/name "chris"
      :account/pass "hello"
      :account/business {:# {:nss #{:business}}
                         :business/name "cleanco"}})


(def p3-geni
  (add-idents
   {:game {:name  [{:type    :string}]
           :score [{:type    :long
                    :default 0}]}
    :profile {:avatar [{:type    :keyword
                        :default :human}]}}))

(fact "checking defaults"
  (process {} p3-geni)
  => {:# {:nss #{}}}

  (process {:game {:name "adam"}}
              p3-geni)
  => {:# {:nss #{:game}}
      :game/name "adam"
      :game/score 0}


  (process {:profile {} :game {}} p3-geni
           {:defaults? true})
  => {:# {:nss #{:profile :game}}
      :profile/avatar :human
      :game/score 0}

  (process {:game {:name "adam"} :profile {}}
              p3-geni {:defaults? true})
  => {:# {:nss #{:profile :game}}
      :game/name "adam"
      :game/score 0
      :profile/avatar :human}

  (process {:profile {} :game {}} p3-geni
           {:defaults? true})
  => {:# {:nss #{:profile :game}}
      :profile/avatar :human
      :game/score 0})


(def p4-geni
  (add-idents
   {:link  {:next  [{:type        :ref
                     :ref-ns      :link}]}
    :value [{:type   :long}]}))


(fact "process across refs"
  (process {:value 1} p4-geni)
  => {:# {:nss #{:value}}
      :value 1}

  (process {:value 1
            :link {:next {:+ {:value 2}}}}
              p4-geni)
  => {:# {:nss #{:value :link}}
      :value 1
      :link/next {:# {:nss #{:link :value}}
                  :value 2}}

   (process {:+/value 1
             :link/next {:+/value 2}}
              p4-geni)
   => {:# {:nss #{:value :link}}
       :value 1
       :link/next {:# {:nss #{:value :link}}
                   :value 2}}

  (process {:value 1
            :link {:next {:+ {:value 2}
                          :next {:+ {:value 3}
                                 :next {:+ {:value 4}}}}}}
              p4-geni)
  => {:# {:nss #{:value :link}}
      :value 1
      :link/next {:# {:nss #{:value :link}}
                  :value 2
                  :link/next {:# {:nss #{:value :link}}
                              :value 3
                              :link/next {:# {:nss #{:value :link}}
                                          :value 4}}}})



(def p5-geni
  (add-idents
   {:account {:name    [{:type        :string}]
              :tags    [{:type        :string
                         :default     "personal"
                         :cardinality :many}]
              :contacts [{:type        :ref
                          :ref-ns      :account.address
                          :cardinality :many}]}
    :account.address
             {:type    [{:type        :keyword
                         :default     :email}]
              :value   [{:type        :string}]}}))

(fact "process will produces the most basic data structures"
  (process {:account {}} p5-geni)
  => {:# {:nss #{:account}}
      :account/tags #{"personal"}}

  (process {:account.address {}} p5-geni)
  => {:# {:nss #{:account.address}}
      :account.address/type :email}

  (process {:account {:name "chris"
                             :tags #{"work" "business"}
                             :contacts #{{:value "z@caudate.me"}
                                         {:type :skype :value "zcaudate"}}}}
                  p5-geni)
  => {:# {:nss #{:account}}
      :account/name "chris",
      :account/tags #{"work" "business"}
      :account/contacts #{{:# {:nss #{:account.address}}
                           :account.address/type :email,
                           :account.address/value "z@caudate.me"}
                          {:# {:nss #{:account.address}}
                           :account.address/type :skype,
                           :account.address/value "zcaudate"}}}

  (process {:account {:name "chris"
                      :contacts #{{:value "z@caudate.me"}}}}
           p5-geni
           {:defaults? false})
  => {:# {:nss #{:account}}
      :account/name "chris"
      :account/contacts #{{:# {:nss #{:account.address}}
                           :account.address/value "z@caudate.me"}}})
