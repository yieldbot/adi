(ns adi.data.samples
  (:use [adi.emit.process :only [process-init-env]]))

(def s1-sgeni {:node {:value  [{}]
                      :parent [{:type :ref
                                :ref  {:ns :node
                                       :rval :children}}]}})

(def s1-env (process-init-env s1-sgeni {}))


(def s2-sgeni {:ns1 {:value [{}]
                     :next  [{:type :ref
                              :ref  {:ns :ns2
                                     :rval :prev}}]}
               :ns2 {:value [{}]
                     :next  [{:type :ref
                              :ref  {:ns :ns1
                                     :rval :prev}}]}})

(def s2-env (process-init-env s2-sgeni {}))

(def s3-sgeni {:account {:id       [{:type :long}]
                         :business {:id   [{:type :long}]
                                    :name [{:type :string}]}
                         :user {:id    [{:type :long}]
                                :name  [{:type :string}]}}})

(def s3-env (process-init-env s3-sgeni {}))

(def s4-sgeni {:ns1 {:valA  [{:default "A1"}]
                     :valB  [{:default (fn [] "B1")}]}
               :ns2 {:valA  [{:default "A2"}]
                     :valB  [{:default (fn [] "B2")}]}})

(def s4-env (process-init-env s4-sgeni {}))
  

(def s5-sgeni {:nsA {:sub1 {:val [{:default "A_1"}]}
                     :sub2 {:val [{:default "A_2"}]}
                     :val1  [{:default "A1"}]
                     :val2  [{:default "A2"}]}
               :nsB {:sub1 {:val [{:default "B_1"}]}
                     :sub2 {:val [{:default "B_2"}]}
                     :val1  [{:default "B1"}]
                     :val2  [{:default "B2"}]}})

(def s5-env (process-init-env s5-sgeni {}))

(def s6-sgeni {:node {:value  [{:default "undefined"}]
                      :parent [{:type :ref
                                :ref  {:ns :node
                                       :rval :children}}]}})

(def s6-env (process-init-env s6-sgeni {}))
  

(def s7-sgeni {:account {:id   [{:type     :long
                                 :required true}]
                         :name [{}]
                         :tags [{:cardinality :many}]}})

(def s7-env (process-init-env s7-sgeni {}))
  
  

(def d1-env
  (process-init-env {:node {:value  [{:default "undefined"}]
                            :parent [{:type :ref
                                      :ref  {:ns :node
                                             :rval :children}}]}
                     :leaf {:value [{:default "leafy"}]
                          :node  [{:type :ref
                                   :ref {:ns :node
                                         :rval :leaves}}]}}))

(def d1-fgeni (-> d1-env :schema :fgeni))

