(ns crystaluniverse.datamaps)

(def magento
  {:magento
   {:customer    {:id           [{:type        :long
                                  :unique      :value}]
                  :industry     [{:type        :string}]}
    :product     {:id           [{:type        :long
                                  :unique      :value}]
                  :categories   [{:type        :long
                                  :cardinality :many}]
                  :active       [{:type        :boolean}]
                  :type         [{:type        :keyword}]}
    :category    {:id           [{:type        :long
                                  :unique      :value}]}}})

(def customer
  {:customer
   {:username    [{:type        :string}]
    :hash        [{:type        :string}]
    :joined      [{:type        :instant}]
    :isActivated [{:type        :boolean}]
    :isVerified  [{:type        :boolean}]
    :firstName   [{:type        :string}]
    :lastName    [{:type        :string}]
    :email       [{:type        :string}]
    :contacts    [{:type        :ref
                   :ref-ns      :customer.contact
                   :cardinality :many}]

    :business   {:name     [{:type         :string}]
                 :abn      [{:type         :string}]
                 :desc     [{:type         :string}]
                 :industry [{:type         :string
                             :cardinality  :many}]}

    :address    {:billing  [{:type        :ref
                             :ref-ns      :customer.address}]
                 :shipping [{:type        :ref
                             :ref-ns      :customer.address}]
                 :all      [{:type        :ref
                             :ref-ns      :customer.address
                             :cardinality :many}]}}
   :customer.address
   {:country   [{:type        :string}]
    :region    [{:type        :string}]
    :city      [{:type        :string}]
    :line1     [{:type        :string}]
    :line2     [{:type        :string}]
    :postcode  [{:type        :string}]}

   :customer.contact
   {:type      [{:type        :keyword}]
    :field     [{:type        :string}]}})

(def category
  {:category
   {:name         [{:type        :string}]
    :enabled      [{:type        :boolean}]
    :postion      [{:type        :long}]
    :children     [{:type        :ref
                    :ref-ns      :category
                    :cardinality :many}]}})

(def product
  {:product
   {:sku           [{:type        :string}]
    :name          [{:type        :string}]
    :slug          [{:type        :string}]
    :enabled       [{:type        :boolean}]
    :price         [{:type        :bigdec}]
    :tags          [{:type        :string
                     :cardinality :many}]
    :desc          {:long       [{:type         :string}]
                    :short      [{:type         :string}]
                    :unit       [{:type         :keyword}]
                    :weight     [{:type         :bigdec}]}
    :images        [{:type        :ref
                     :ref-ns      :product.image
                     :cardinality :many}]

    :variants      {:singles    [{:type         :ref
                                  :ref-ns       :product.single
                                  :cardinality  :many}]
                    :groups     [{:type         :ref
                                  :ref-ns       :product.group
                                  :cardinality  :many}]
                    :matrices   [{:type         :ref
                                  :ref-ns       :product.matrix
                                  :cardinality  :many}]}}

   :product.single
   {:sku           [{:type        :string}]
    :price         [{:type        :bigdec}]
    :enabled       [{:type        :boolean}]
    :name          [{:type        :string}]
    :postion       [{:type        :long}]}

   :product.group
   {:sku           [{:type        :string}]
    :price         [{:type        :bigdec}]
    :enabled       [{:type        :boolean}]
    :name          [{:type        :string}]
    :postion       [{:type        :long}]
    :singles       [{:type        :ref
                     :ref-ns      :product.singles
                     :cardinality  :many}]}

   :product.matrix
   {:sku           [{:type        :string}]
    :price         [{:type        :bigdec}]
    :enabled       [{:type        :boolean}]
    :name          [{:type        :string}]
    :postion       [{:type        :long}]
    :singles       [{:type        :ref
                     :ref-ns      :product.group
                     :cardinality  :many}]
    :exceptions    [{:type        :string
                     :cardinality  :many}]}

   :product.image
   {:file        [{:type        :string}]
    :label       [{:type        :string}]
    :url         [{:type        :string}]
    :postion     [{:type        :long}]
    :enabled     [{:type        :boolean}]
    :tags        [{:type        :string
                   :cardinality :many}]}})

(def all (merge magento customer product category))
