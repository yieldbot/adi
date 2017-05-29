(ns spirit.test.examples)

(def account-name-age-sex
  {:account {:name [{}]
             :age  [{:type :long}]
             :sex  [{:type :enum
                     :enum {:ns :account.sex
                            :values #{:m :f}}}]}})

(def link-value-next
  {:link {:value [{}]
          :next  [{:type :ref
                   :ref {:ns :link
                         :rval :prev}}]}})

(def account-orders-items-image
 {:account {:user     [{:representative true}]
            :tags     [{:cardinality :many}]}
  :order   {:account [{:type :ref
                         :ref {:ns :account}}]
              :number [{:type :long}]}
  :item    {:name   [{}]
            :order  [{:type :ref
                      :ref {:ns :order}}]}
  :image   {:item  [{:type :ref
                      :ref {:ns :item}}]
            :url   [{}]}})