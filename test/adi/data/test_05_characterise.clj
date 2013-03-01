(ns adi.data.test-05-characterize.clj
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

(def category-data-1
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

(def category-data-2
  {:category {:name "root"
              :tags #{"shop" "new"}
              :image {:url "www.example.com/root" :type :big}
              :children #{{:name "crystals"
                           :tags #{"shiny"}
                           :image {:url "www.example.com/crystals" :type :big}
                           :children #{{:name "rose"}
                                       {:name "jasmin"}}}
                          {:name "flowers"
                           :tags #{"perfume" "red"}}}}})

(def category-data-3
  {:category {:name "root"
              :tags #{"shop" "new"}
              }})

(pprint
 (ad/characterise category-map
                  (ad/process category-map category-data-3)
                  {:generate-ids false}))

(pprint
 (ad/characterise category-map
                  (ad/process category-map category-data-2 {:add-defaults? false})
                  {:generate-ids false}))


(fact
  (ad/characterise category-map
                   (ad/process category-map category-data-1 {:add-defaults? false})
                   {:generate-ids false})
  => {:data-one {:category/name "root"},
      :refs-many
      {:category/children
       #{{:data-one {:category/name "flowers"},
          :refs-many
          {:category/children
           #{{:data-one {:category/name "rose"}}
             {:data-one {:category/name "jasmin"}}}}}
         {:data-one {:category/name "crystals"},
          :refs-many
          {:category/children
           #{{:data-one {:category/name "quartz"},
              :refs-many
              {:category/children
               #{{:data-one {:category/name "smokey"}}
                 {:data-one {:category/name "clear"}}}}}
             {:data-one {:category/name "citrine"}}
             {:data-one {:category/name "aquamarine"}}}}}}}})
