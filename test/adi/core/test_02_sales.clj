(ns adi.core.test_02_sales)

(def account-map
  {:account {:username   [{:type        :string}]
             :password   [{:type        :string}]
             :tags       [{:type        :string
                           :cardinality :many}]
             :address   {:line1    [{:type        :string}]
                         :line2    [{:type        :string}]
                         :city     [{:type        :string}]
                         :region   [{:type        :string}]
                         :country  [{:type        :string}]
                         :postcode [{:type        :string}]}}})

(def account-recs
  [{:account {:username "chris"
              :password "hello"
              :tags     #{"java", "clojure", "javascript"}
              :address  {:line1   "123 Sesame St"
                         :line2   ""
                         :city    "Big Bird"
                         :region  "Colphon Heights"
                         :country "Gatimo"}}}])

(def sales-map
  {:sales
   {:isVip     [{:type        :boolean}]
    :orders    [{:type        :ref
                 :ref-ns      :order
                 :cardinality :many}]}
   :order
   {:id        [{:type        :string}]
    :entries   [{:type        :ref
                 :ref-ns      :order.entry
                 :cardinality :many}]
    :total     [{:type        :bigdec}]}

   :order.entry
   {:position  [{:type        :long}]
    :product   [{:type        :ref
                 :ref-ns      :product}]
    :number    [{:type        :bigint}]
    :price     [{:type        :bigdec}]
    :discount  [{:type        :float}]}

   :product
   {:sku       [{:type        :string}]
    :name      [{:type        :string}]
    :slug      [{:type        :string}]
    :desc      [{:type        :string}]
    :price     [{:type        :bigdec}]}})

(defn get-required-keys [dm kns]
  ())

(get-required-keys sales-map kns)

(def product-recs
  [{:+ {:product {:sku  "1234567"
                  :name "Selenite Sphere"
                  :slug "sphere-selenite"
                  :desc "3-inch sphere. Very Awesome"
                  :price (bigdec 0.45)
                  ;;:price 0.45
                  }}}])

(def order-recs
  [{:order
    {:id      "asc00002"
     :total   (bigdec 1034.45)
     :entries #{[{:position 0
                  :product [{:sku  "123"
                             :name "Selenite Sphere"
                             :slug "sphere-selenite"
                             :desc "3-inch sphere. Very Awesome"
                             :price (bigdec 4.50)}]
                  :number   10
                  :discount 0.1}]
                [{:position 1
                  :product [{:sku  "234"
                             :name "Petalite Sphere"
                             :slug "sphere-petalite"
                             :desc "1-inch sphere. White"
                             :price (bigdec 40.0)}]
                  :number   20
                  :discount 0.3}]}}}])

(def category-map
  {:category {:children  [{:type        :ref
                           :ref-ns      :category
                           :cardinality :many}]
              :alt       [{:type        :ref
                           :ref-ns      :import}]
              :tags      [{:type        :string
                           :cardinality :many}]
              :value     [{:type        :string
                          :required     true}]}

   :import   {:id        [{:type        :long
                           :required    true}]
              :flag      [{:type        :boolean}]}})

(def category-rec
  {:category
   {:+ {:import {:id 1
                 :flag true}}
    :value "root"
    :alt [{:+ {:import {:id 10}}
           :flag true}]
    :tags #{"crystals"}
    :children
    #{[{:+ {:import {:id 11}}
        :value "jewels"
        :children
        #{[{:value "bracelets"}]
          [{:value "necklaces"}]
          [{:value "headpieces"}]}}]
      [{:+ {:import {:id 19}}
        :value "polished"
        :children
        #{[{:value "spheres"}]
          [{:value "pyramids"}]
          [{:value "free forms"}]}}]}}})
