(ns adi.test-schema
  (:use midje.sweet
        adi.utils
        adi.schema
        adi.checkers))

(fact "infer-idents"
  (infer-idents {:a [{}]})
  => {:a [{:ident :a}]}

  (infer-idents {:a [{}]
               :b [{}]})
  => {:a [{:ident :a}]
      :b [{:ident :b}]}

  (infer-idents {:a/b [{}]
               :a/c [{}]})
  => {:a {:b [{:ident :a/b}]
          :c [{:ident :a/c}]}}

  (infer-idents {:a {:b [{}]
                   :c [{}]}})
  => {:a {:b [{:ident :a/b}]
          :c [{:ident :a/c}]}})


(def s1a-geni
  (infer-idents {:account {:email [{:type    :ref
                                    :ref-ns  :email}]}}))

(def s2-geni
  (infer-idents {:account {:bigImage    [{:type    :ref
                                          :ref-ns  :image}]
                           :smallImage  [{:type    :ref
                                          :ref-ns  :image}]}}))
(def s3-geni
  (infer-idents {:account {:profile/image    [{:type  :ref :ref-ns  :image}]
                           :main/image       [{:type  :ref :ref-ns  :image}]}}))




(fact "infer-reverse-refs"
  (infer-reverse-refs s1a-geni)
  => {:account {:email     [{:type        :ref
                             :ref-ns      :email
                             :ident       :account/email}]}
      :email   {:_accounts [{:type        :ref
                             :ref-key     :account/_email
                             :ref-ns      :account
                             :cardinality :many
                             :ident       :email/_accounts}]}}

  (infer-reverse-refs s2-geni)
  => {:account  {:bigImage      [{:type      :ref
                                  :ref-ns    :image
                                  :ident     :account/bigImage}]
                 :smallImage    [{:type      :ref
                                  :ref-ns    :image
                                  :ident     :account/smallImage}]}
      :image    {:_bigImage_accounts    [{:type        :ref
                                          :ref-key     :account/_bigImage
                                          :ref-ns      :account
                                          :cardinality :many
                                          :ident       :image/_bigImage_accounts}]
                 :_smallImage_accounts  [{:type        :ref
                                          :ref-key     :account/_smallImage
                                          :ref-ns      :account
                                          :cardinality :many
                                          :ident       :image/_smallImage_accounts}]}}

  (infer-reverse-refs s2-geni)
  => {:account  {:big    {:image  [{:type      :ref
                                    :ref-ns    :image
                                    :ident     :account/big/image}]}
                 :small  {:image  [{:type      :ref
                                    :ref-ns    :image
                                    :ident     :account/small/image}]}}
      :image    {:_big_image_accounts    [{:type        :ref
                                           :ref-key     :account/big/_image
                                           :ref-ns      :account
                                           :cardinality :many
                                           :ident       :image/_big_image_accounts}]
                 :_small_image_accounts  [{:type        :ref
                                           :ref-key     :account/small/_image
                                           :ref-ns      :account
                                           :cardinality :many
                                           :ident       :image/_small_image_accounts}]}})

(def rv2-geni
  {:account  {:big    {:image  [{:type      :ref
                                    :ref-ns    :image
                                    :ident     :account/bigImage}]}
                 :small  {:image  [{:type      :ref
                                    :ref-ns    :image
                                    :ident     :account/smallImage}]}}
      :image    {:_big_image_accounts    [{:type        :ref
                                           :ref-key     :account/big/_image
                                           :ref-ns      :account
                                           :cardinality :many
                                           :ident       :image/_big_image_accounts}]
                 :_small_image_accounts  [{:type        :ref
                                           :ref-key     :account/small/_image
                                           :ref-ns      :account
                                           :cardinality :many
                                           :ident       :image/_small_image_accounts}]}})

(find-reverse-idents (flatten-keys-in rv2-geni))

(remove-empty-in (dissoc-keys-in rv2-geni #{:_big_image_accounts :_small_image_accounts}))


(make-reverse-meta-single [{:type      :ref
                            :ref-ns    :image
                            :ident     :account/smallImage}])
