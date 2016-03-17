(def schema
  {:book   {:name    [{:required true
                       :fulltext true}]
            :author  [{:fulltext true}]}
   
   :inventory {:count [{:type :long}]
               :cover [{:type :enum
                        :enum {:ns :cover.type
                               :values #{:hard :soft}}}]
               :book    [{:type :ref
                          :ref  {:ns :book}}]
               :store   [{:type :ref
                          :ref  {:ns :store}}]}
   :store  {:name    [{:required true
                       :fulltext true}]
            :address [{:type :ref
                       :ref  {:ns :address}}]}

   :address {:line-1  [{}]
             :line-2  [{}]
             :city    [{}]
             :region  [{}]
             :country [{:required true}]}})

(def schema-link
  {:node {:value  [{}]
          :root   [{:type :boolean}]
          :parent [{:type :ref
                    :ref {:ns :node
                          :rval :children}}]}})

(def ds (adi/connect! "datomic:mem://adi-guide-links" schema-link true true))

(adi/insert! ds {:node {:value "1"
                        :root true
                        :children #{{:value "2"}
                                    {:value "3"}}}}
             {:options {:transact :promise}})

(adi/delete! ds :node/value {:raw true})

(adi/delete! ds :node/value)


(adi/select ds :node/root
            {:pull {:node {:children :yield}}
             :options {:ids :true}}
            
            )



#{{:node {:value "1", :root true, :children #{{:value "2", :+ {:db {:id 17592186045424}}}
                                              {:value "3", :+ {:db {:id 17592186045423}}}}},
   :db {:id 17592186045422}}
  {:node {:value "1", :root true, :children #{{:value "3", :+ {:db {:id 17592186045419}}}
                                              {:value "2", :+ {:db {:id 17592186045420}}}}},
   :db {:id 17592186045418}}}

(adi/select ds {:node/value "1"}
            :pull {:node {:children :yield
                          :parent :id}}
            ;;:first
            ;;:return :entities
            )

;; Options for Query

:return #{:ids :entities :data}

:raw #{true false}

:transact #{:datomic :async :resolve :promise}

(adi/update! ds
             {:node {:value "1"}}
             {:node/value "11"}
             :options {:simulate true}
             :raw
             )









(comment
  
  (def ds (adi/connect! "datomic:mem://adi-guide" schema true true))

  (:schema ds)
  => #schema{:store {:address :&address, :name :string, :inventories :&inventory<*>}, :address {:stores :&store<*>, :line-2 :string, :country :string, :region :string, :line-1 :string, :city :string}, :inventory {:book :&book, :cover :enum, :count :long, :store :&store}, :book {:author :string, :name :string, :inventories :&inventory<*>}}

  (:connection ds)
  => #connection{1000 #inst "2016-03-08T09:17:55.064-00:00"}

  (adi/insert! ds [{:book {:name "Count of Monte Cristo"
                           :author "Alexander Dumas"}}])

  (adi/select ds :book :ids)

  (adi/select ds {:book {:name '(.startsWith ? "Count")}} :ids)
  #{{:book {:author "Alexander Dumas", :name "Count of Monte Cristo"}, :db {:id 17592186045420}}
    {:book {:author "Alexander Dumas", :name "Count of Monte Cristo"}, :db {:id 17592186045422}}}

  (adi/select ds {:book {:name '(.startsWith ? "Count")}})
  
  (adi/insert! (assoc ds
                      :options {;;:ban-expressions true
                                :ids true})
               [{:book {:name "The Magicians"
                        :author "Lev Goodman"}}])
  (adi/delete! ds {:book/name "The Magicians"})
  (adi/delete! ds {:book/name '_})
  

  (adi/select ds :book)
  => #{{:book {:author "Alexander Dumas", :name "Count of Monte Cristo"}}}

  (adi/select ds 17592186045422 :ban-top-id)
  (adi/select ds 17592186045422 :ban-body-ids)
  (adi/select ds 17592186045422 :ban-ids)
  
  )

(-> ds :schema :tree :store :name)

(-> ds :schema :flat)
