(ns adi.data.test-03-process-refs-more
  (:use midje.sweet
        adi.utils
        adi.checkers)
  (:require [adi.data :as ad]))


(def account-map
  (flatten-keys
   {:account {:name    [{:type        :string}]
              :tags    [{:type        :string
                         :default     #{"personal"}
                         :cardinality :many}]
              :contacts [{:type        :ref
                          :ref-ns      :account.address
                          :cardinality :many}]}
    :account.address
             {:type    [{:type        :keyword
                         :default     :email}]
              :value   [{:type        :string}]}}))

(fact "process will produces the most basic data structures"
  (ad/process account-map {:account {}})
  => {:account/tags #{"personal"}}

        (ad/process account-map {:account.address {}})
        => {:account.address/type :email}

  (ad/process account-map
                  {:account {:name "chris"
                             :tags #{"work" "business"}
                             :contacts #{{:value "z@caudate.me"}
                                         {:type :skype :value "zcaudate"}}}})
  => {:account/name "chris",
      :account/tags #{"work" "business"}
      :account/contacts #{{:account.address/type :email,
                               :account.address/value "z@caudate.me"}
                              {:account.address/type :skype,
                               :account.address/value "zcaudate"}}}

  (ad/process account-map
              {:account {:name "chris"
                         :contacts #{{:value "z@caudate.me"}}}}
              {:add-defaults? false})
  => {:account/name "chris"
      :account/contacts #{{:account.address/value "z@caudate.me"}}})

(def category-map
  (flatten-keys
   {:category
    {:name         [{:type        :string}]
     :enabled      [{:type        :boolean
                     :default     true}]
     :priority     [{:type        :long
                     :default     0}]
     :children     [{:type        :ref
                     :ref-ns      :category
                     :cardinality :many}]}}))

(def category-data
  {:category {:name "root"
                          :children #{{:name "crystals"
                                       :children #{{:name "quartz"
                                                    :children #{{:name "smokey"}
                                                                {:name "clear"}}}
                                                   {:name "aquamarine"}
                                                   {:name "citrine"}}}
                                      {:name "flowers"
                                       :children #{{:name "rose"}
                                                   {:name "jasmin"}}}}}})


(fact "testing on nested refs"
    (ad/process category-map category-data {:add-defaults? false})
  => {:category/name "root"
      :category/children
      #{{:category/name "crystals"
         :category/children
         #{{:category/name "quartz"
            :category/children
            #{{:category/name "smokey"}
              {:category/name "clear"}}}
           {:category/name "aquamarine"}
           {:category/name "citrine"}}}
        {:category/name "flowers"
         :category/children
         #{{:category/name "jasmin"}
           {:category/name "rose"}}}}}

  (ad/process category-map category-data)
  => {:category/name "root"
      :category/enabled true
      :category/priority 0
      :category/children
      #{{:category/name "crystals"
         :category/enabled true
         :category/priority 0
         :category/children
         #{{:category/name "quartz"
            :category/enabled true
            :category/priority 0
            :category/children
            #{{:category/name "smokey"
               :category/enabled true
               :category/priority 0}
              {:category/name "clear"
               :category/enabled true
               :category/priority 0}}}
           {:category/name "aquamarine"
            :category/enabled true
            :category/priority 0}
           {:category/name "citrine"
            :category/enabled true
            :category/priority 0}}}
        {:category/name "flowers"
         :category/enabled true
         :category/priority 0
         :category/children
         #{{:category/name "jasmin"
            :category/enabled true
            :category/priority 0}
           {:category/name "rose"
            :category/enabled true
            :category/priority 0}}}}})
