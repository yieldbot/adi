(ns adi.example.schema)

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
