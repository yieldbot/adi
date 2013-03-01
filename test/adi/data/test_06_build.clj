(ns adi.data.test-06-build
  (:use midje.sweet
        adi.utils
        adi.checkers)
  (:require [adi.data :as ad]))

(def category-map
  (flatten-keys
   {:category {:name         [{:type        :string}]
               :tags         [{:type        :string
                               :cardinality :many}]
               :image        [{:type        :ref
                               :ref-ns      :image}]
               :children     [{:type        :ref
                               :ref-ns      :category
                               :cardinality :many}]}
    :image     {:url         [{:type        :string}]
                :type        [{:type        :keyword}]}}))

(def category-data
  {:db/id 1000001
   :category {:name "root"
              :children #{{:+/db/id 1000002
                           :name "crystals"
                           :children #{{:+/db/id 1000003
                                        :name "quartz"
                                        :children #{{:+/db/id 1000004
                                                     :name "smokey"}
                                                    {:+/db/id 1000005
                                                     :name "clear"}}}
                                       {:+/db/id 1000006
                                        :name "aquamarine"}
                                       {:+/db/id 1000007
                                        :name "citrine"}}}
                          {:+/db/id 1000008
                           :name "flowers"
                           :children #{{:+/db/id 1000009
                                        :name "rose"}
                                       {:+/db/id 1000010
                                        :name "jasmin"}}}}}})

(fact "build will make the datomic structure"
  (-> {:db/id 1000001
       :category {:name "root" :tags #{"shop" "new"}}}
      (ad/process category-map)
      (ad/characterise category-map)
      ad/build)
  => [{:category/name "root", :db/id 1000001}
      [:db/add 1000001 :category/tags "new"]
      [:db/add 1000001 :category/tags "shop"]])


(fact "more complex structures can be created"
 (-> category-data
     (ad/process category-map)
     (ad/characterise category-map)
     ad/build)
 => [{:db/id 1000010, :category/name "jasmin"}
     {:db/id 1000009, :category/name "rose"}
     {:db/id 1000008, :category/name "flowers"}
     {:db/id 1000007, :category/name "citrine"}
     {:db/id 1000006, :category/name "aquamarine"}
     {:db/id 1000004, :category/name "smokey"}
     {:db/id 1000005, :category/name "clear"}
     {:db/id 1000003, :category/name "quartz"}
     {:db/id 1000002, :category/name "crystals"}
     {:db/id 1000001, :category/name "root"}
     [:db/add 1000008 :category/children 1000010]
     [:db/add 1000008 :category/children 1000009]
     [:db/add 1000003 :category/children 1000004]
     [:db/add 1000003 :category/children 1000005]
     [:db/add 1000002 :category/children 1000007]
     [:db/add 1000002 :category/children 1000006]
     [:db/add 1000002 :category/children 1000003]
     [:db/add 1000001 :category/children 1000008]
     [:db/add 1000001 :category/children 1000002]])


(def category-data-2
  {:db/id 1000000
   :category {:name "root"
              :tags #{"shop" "new"}
              :image {:+/db/id 1000001
                      :url "www.example.com/root" :type :big}
              :children #{{:+/db/id 1000002
                           :name "crystals"
                           :tags #{"shiny"}
                           :image {:+/db/id 1000003
                                   :url "www.example.com/crystals" :type :big}
                           :children #{{:+/db/id 1000004
                                        :name "rose"}
                                       {:+/db/id 1000005
                                        :name "jasmin"}}}
                          {:+/db/id 1000006
                           :name "flowers"
                           :tags #{"perfume" "red"}}}}
   :#/not {:category/name "something"}})

(fact "The relationships will be automatically generated"
 (-> category-data-2
            (ad/process category-map)
            (ad/characterise category-map)
            ad/build)
 => [{:db/id 1000001, :image/type :big, :image/url "www.example.com/root"}
     {:db/id 1000003, :image/type :big :image/url "www.example.com/crystals"}
     {:db/id 1000004, :category/name "rose"}
     {:db/id 1000005, :category/name "jasmin"}
     {:db/id 1000002, :category/name "crystals"}
     [:db/add 1000002 :category/tags "shiny"]
     {:db/id 1000006, :category/name "flowers"}
     [:db/add 1000006 :category/tags "perfume"]
     [:db/add 1000006 :category/tags "red"]
     {:db/id 1000000, :category/name "root"}
     [:db/add 1000000 :category/tags "new"]
     [:db/add 1000000 :category/tags "shop"]
     [:db/add 1000002 :category/image 1000003]
     [:db/add 1000002 :category/children 1000004]
     [:db/add 1000002 :category/children 1000005]
     [:db/add 1000000 :category/image 1000001]
     [:db/add 1000000 :category/children 1000002]
     [:db/add 1000000 :category/children 1000006]])

(-> category-data-2
    (ad/process category-map)
    (ad/characterise category-map)
    ad/build)
