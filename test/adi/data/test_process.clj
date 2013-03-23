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

(fact
  (process {:account {:id   2
                      :name "chris"
                      :pass "hello"}}
           p1-geni)
  => {:account/id 2
      :account/name "chris"
      :account/pass "hello"}

  (process {:account {:name "chris"
                      :pass "hello"}}
           p1-geni)
  => {:account/id 1
      :account/name "chris"
      :account/pass "hello"}

  (process {:account {:name "chris"
                      :pass "hello"
                      :business {}}}
           p1-geni)
  => {:account/id 1
      :account/name "chris"
      :account/pass "hello"
      :account/business {:business/id  10}}

  (process {:account {:name "chris"
                      :pass "hello"
                      :business {:name "cleanco"}}}
           p1-geni)
  => {:account/id 1
      :account/name "chris"
      :account/pass "hello"
      :account/business {:business/id   10
                         :business/name "cleanco"}})

(def p2-geni
  (add-idents
   {:account  {:name  [{:type        :string
                        :required    true}]
               :pass  [{:type        :string}]
               :business  [{:type        :ref
                            :ref-ns      :business}]}
    :business {:name  [{:type        :string
                        :required    true}]}}))


(fact
  (process {:account {:name "chris"
                      :pass "hello"}}
           p2-geni
           {:required? true})
  => {:account/name "chris"
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
  => {:account/name "chris"
      :account/pass "hello"
      :account/business {:business/name "cleanco"}})
