(ns adi.data.test-05-characterise
  (:use midje.sweet
        adi.utils
        adi.checkers)
  (:require [adi.data :as ad]))

(def category-map
  (flatten-all-keys
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


(fact "characterise will sort out the data into different piles depending upon options"
  (-> {:category {:name "root" :tags #{"shop" "new"}}}
       (ad/process category-map )
       (ad/characterise category-map {:generate-ids false}))
  => {:data-many {:category/tags #{"new" "shop"}} :data-one {:category/name "root"}}

  (-> {:category {:name "root" :tags #{"shop" "new"}}}
      (ad/process category-map {:sets-only? true})
      (ad/characterise category-map {:generate-ids false}))
  => {:data-many {:category/tags #{"new" "shop"}, :category/name #{"root"}}})

(fact "more complex results"
  (-> category-data-1
      (ad/process category-map  {:add-defaults? false})
      (ad/characterise category-map {:generate-ids false}))
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


(fact "more complex scenarios of data many"
  (-> category-data-2
              (ad/process category-map {:add-defaults? false})
              (ad/characterise category-map {:generate-ids false}))

  {:data-one {:category/name "root"}
            :data-many {:category/tags #{"new" "shop"}}
            :refs-one {:category/image {:data-one {:image/type :big
                                                   :image/url "www.example.com/root"}}}
            :refs-many {:category/children
                        #{{:data-one {:category/name "flowers"}
                           :data-many {:category/tags #{"perfume" "red"}}}
                          {:data-one {:category/name "crystals"}
                           :data-many {:category/tags #{"shiny"}}
                           :refs-one
                           {:category/image {:data-one {:image/type :big
                                                        :image/url "www.example.com/crystals"}}}
                           :refs-many
                           {:category/children
                            #{{:data-one {:category/name "rose"}}
                              {:data-one {:category/name "jasmin"}}}}}}}}


  (-> category-data-2
      (ad/process category-map {:defaults? false :sets-only? true})
      (ad/characterise category-map {:generate-ids false}))

  => {:data-many {:category/tags #{"new" "shop"}, :category/name #{"root"}},
      :refs-many
      {:category/image
       #{{:data-many {:image/type #{:big}, :image/url #{"www.example.com/root"}}}}
       :category/children
       #{{:data-many {:category/tags #{"perfume" "red"} :category/name #{"flowers"}}}
         {:data-many {:category/tags #{"shiny"}, :category/name #{"crystals"}},
          :refs-many {:category/image
                      #{{:data-many
                         {:image/type #{:big},
                          :image/url #{"www.example.com/crystals"}}}},
                      :category/children
                      #{{:data-many {:category/name #{"jasmin"}}}
                        {:data-many {:category/name #{"rose"}}}}}}}}})
