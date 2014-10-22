(ns adi.data.pack.test_analyse
  (:use midje.sweet)
  (:require
   [adi-test.schemas :refer :all]
   [adi-test.checkers :refer :all]
   [adi.data.pack.analyse :as t :refer [analyse]]))


(fact "Normal"
  (analyse {:account {:name "Chris"}}
           {:schema account-name-age-sex-xm})
  => {:account/name "Chris"}


  "Automatic Coercion"
  (analyse {:account {:name :Chris
                      :age "10"}}
           {:schema account-name-age-sex-xm})
  => {:account/name "Chris", :account/age 10}

  "Ignore extraneous data"
  (analyse {:account {:name :Chris
                      :hello "10"}}
           {:schema account-name-age-sex-xm})
  => (raises-issue {:no-schema true :data "10"})

  "Unless otherwise stated"
  (analyse {:account {:name :Chris
                      :hello "10"}}
           {:schema account-name-age-sex-xm
            :options {:schema-ignore true}})
  => {:account/name "Chris"})

(fact "Symbols"
  (analyse {:account {:name '?x}}
           {:schema account-name-age-sex-xm})
  => '{:account/name ?x}

  (analyse {:db {:id '?y}
            :account {:name '?x}}
           {:schema account-name-age-sex-xm})
  => '{:# {:id ?y}, :account/name ?x})


(fact "Ids"
  (analyse {:db {:id 2}
            :account {:name "Chris"}}
           {:schema account-name-age-sex-xm})
  => {:# {:id 2}, :account/name "Chris"})

(fact "Ban Ids"
  (analyse {:db {:id 2}
            :account {:name "Chris"}}
           {:schema account-name-age-sex-xm
            :options {:ban-ids true}})
  => (raises-issue {:id-banned true :id 2})

  (analyse {:db {:id 2}
            :account {:name "Chris"}}
           {:schema account-name-age-sex-xm
            :options {:ban-top-id true}})
  => (raises-issue {:top-id-banned true :id 2})

  (analyse {:db {:id 2}
            :account {:name "Chris"}}
           {:schema account-name-age-sex-xm
            :options {:ban-body-ids true}})
  => {:# {:id 2}, :account/name "Chris"}

  (analyse {:account {:user "Chris"
                      :orders #{{:+ {:db {:id 3}}
                                 :number 1}}}}
           {:schema account-orders-items-image-xm
            :options {:ban-body-ids true}})
  => (raises-issue {:body-id-banned true :id 3}))

(fact "Ban Ids for Refs"
  (analyse {:account {:user "Chris"
                      :orders #{1000}}}
           {:schema account-orders-items-image-xm
            :options {:ban-body-ids true}})
  => (raises-issue {:body-id-banned true}))

(fact "Ban Expressions"
  (analyse {:account {:user '(= "Chris")}}
           {:schema account-orders-items-image-xm})
  => '{:account/user (= "Chris")}

  (analyse {:account {:user '(= "Chris")}}
           {:schema account-orders-items-image-xm
            :options {:ban-expressions true}})
  => (raises-issue {:expression-banned true :data '(= "Chris")})

  (analyse {:account {:user "Chris"}}
           {:schema account-orders-items-image-xm
            :options {:ban-expressions true}})
  => {:account/user "Chris"}

  (analyse {:account {:orders #{{:number '(= "5")}}}}
           {:schema account-orders-items-image-xm})
  => {:account/orders #{{:order/number '(= "5")}}}

  (analyse {:account {:orders #{{:number '(= "5")}}}}
           {:schema account-orders-items-image-xm
            :options {:ban-expressions true}})
  => (raises-issue {:expression-banned true :data '(= "5")}))

(fact "Reverse Analysis"
  (analyse {:order {:account #{{:user "Chris"}}}}
           {:schema account-orders-items-image-xm})
  => {:order/account #{{:account/user "Chris"}}})
