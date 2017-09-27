(ns adi.schema.find-test
  (:use midje.sweet)
  (:require [adi.schema.find :refer :all]))

^{:refer adi.schema.find/all-attrs :added "0.3"}
(fact "finds all attributes satisfying `f` in a schema"

  (all-attrs
   {:account/number [{:ident   :account/number
                      :type    :long}]
    :account/date   [{:ident   :account/date
                      :type    :instant}]}
   (fn [attr] (= (:type attr) :long)))
  => {:account/number [{:type :long, :ident :account/number}]})

^{:refer adi.schema.find/all-idents :added "0.3"}
(fact "finds all idents satisfying `f` in a schema"

  (all-idents
   {:account/number [{:ident   :account/number
                      :type    :long}]
    :account/date   [{:ident   :account/date
                      :type    :instant}]}
   (fn [attr] (= (:type attr) :long)))
  => [:account/number])

^{:refer adi.schema.find/is-reverse-ref? :added "0.3"}
(fact "predicate for reverse ref"

  (is-reverse-ref? {:ident  :email/accounts
                    :type   :ref
                    :ref    {:type :reverse}})
  => true)
