(ns adi.data.test-characterise
  (:use adi.data
        adi.schema
        adi.utils
        midje.sweet))

(def c1-geni
  (add-idents
   {:category {:name         [{:type        :string}]
               :tags         [{:type        :string
                               :cardinality :many}]
               :image        [{:type        :ref
                               :ref-ns      :image}]
               :children     [{:type        :ref
                               :ref-ns      :category
                               :cardinality :many}]}
    :image    {:url         [{:type        :string}]
               :type        [{:type        :keyword}]}}))


(fact "characterise will sort out the data into different piles depending upon options"
  (-> {:category {:name "root" :tags #{"shop" "new"}}}
       (process c1-geni)
       (characterise (flatten-all-keys c1-geni) {:generate-ids false}))
  => {:# {:nss #{:category}}
      :data-many {:category/tags #{"new" "shop"}}
      :data-one {:category/name "root"}}

  (-> {:category {:name "root" :tags #{"shop" "new"}}}
      (process c1-geni {:sets-only? true})
      (characterise (flatten-all-keys c1-geni) {:generate-ids false}))
  => {:# {:nss #{:category}}
      :data-many {:category/tags #{"new" "shop"}
                  :category/name #{"root"}}})


(def c1-data
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

(fact "more complex results"
  (-> c1-data
      (process c1-geni {:defaults? false})
      (characterise (flatten-all-keys c1-geni) {:generate-ids false}))
  =>
  {:# {:nss #{:category}}
   :data-one {:category/name "root"}
   :refs-many
   {:category/children
    #{{:# {:nss #{:category}}
       :data-one {:category/name "flowers"}
       :refs-many
       {:category/children
        #{{:# {:nss #{:category}}
           :data-one {:category/name "rose"}}
          {:# {:nss #{:category}}
           :data-one {:category/name "jasmin"}}}}}
      {:# {:nss #{:category}}
       :data-one {:category/name "crystals"},
       :refs-many
       {:category/children
        #{{:# {:nss #{:category}}
           :data-one {:category/name "quartz"}
           :refs-many
           {:category/children
            #{{:# {:nss #{:category}}
               :data-one {:category/name "smokey"}}
              {:# {:nss #{:category}}
               :data-one {:category/name "clear"}}}}}
          {:# {:nss #{:category}}
           :data-one {:category/name "citrine"}}
          {:# {:nss #{:category}}
           :data-one {:category/name "aquamarine"}}}}}}}})


(def c2-data
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

(fact "more complex scenarios of data many"
  (-> c2-data
      (process c1-geni {:defaults? false})
      (characterise (flatten-all-keys c1-geni) {:generate-ids false}))
  =>
  {:# {:nss #{:category}}
   :data-many {:category/tags #{"new" "shop"}}
   :data-one {:category/name "root"}
   :refs-many
   {:category/children
    #{{:# {:nss #{:category}}
       :data-many {:category/tags #{"perfume" "red"}}
       :data-one {:category/name "flowers"}}
      {:# {:nss #{:category}}
       :data-many {:category/tags #{"shiny"}}
       :data-one {:category/name "crystals"}
       :refs-many
       {:category/children
        #{{:# {:nss #{:category}}
           :data-one {:category/name "rose"}}
          {:# {:nss #{:category}}
           :data-one {:category/name "jasmin"}}}}
       :refs-one
       {:category/image
        {:# {:nss #{:image}}
         :data-one
         {:image/url "www.example.com/crystals"
          :image/type :big}}}
       }}}
   :refs-one
   {:category/image
    {:# {:nss #{:image}}
     :data-one {:image/url "www.example.com/root"
                :image/type :big}}}})


(fact "more complex scenarios of data many"
  (-> c2-data
      (process c1-geni {:defaults? false :sets-only? true})
      (characterise (flatten-all-keys c1-geni) {:generate-ids false}))
  =>
  {:# {:nss #{:category}}
   :data-many {:category/tags #{"new" "shop"}
               :category/name #{"root"}}
   :refs-many
   {:category/children
    #{{:# {:nss #{:category}}
       :data-many
       {:category/tags #{"shiny"}
        :category/name #{"crystals"}}
       :refs-many
       {:category/children
        #{{:# {:nss #{:category}}
           :data-many {:category/name #{"jasmin"}}}
          {:# {:nss #{:category}}
           :data-many {:category/name #{"rose"}}}}
        :category/image
        #{{:# {:nss #{:image}}
           :data-many
           {:image/url #{"www.example.com/crystals"}
            :image/type #{:big}}}}}}
      {:# {:nss #{:category}}
       :data-many
       {:category/tags #{"perfume" "red"}
        :category/name #{"flowers"}}}}
    :category/image
    #{{:# {:nss #{:image}}
       :data-many
       {:image/url #{"www.example.com/root"}
        :image/type #{:big}}}}}})
