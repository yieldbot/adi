(ns adi.core.test_select
  (:use midje.sweet)
  (:require
   [adi-test.schemas :refer :all]
   [adi-test.checkers :refer :all]
   [adi.data :refer :all]
   [adi.model :as am]
   [adi.core :refer [select select-ids
                     select-entities connect-env!
                     insert! delete! update! retract!
                     update-in! delete-in! retract-in!
                     sync-> iid delete-all!
                     q selectq selectq-entities selectq-ids transact!]]
   [datomic.api :as d]))

(def ^:dynamic uri "datomic:mem://adi-test-select")
(def env (connect-env! uri account-orders-items-image true))
(def DATA [{:account
            {:user "Hello"
             :orders {:number 1}}}
           {:account
            {:user "Chris"
             :orders {:number 2}}}])

(def DATA2 [{:order {:number 1
                    :account {:user "Hello"
                              :orders {:number 2}}}}
            {:account
             {:user "Chris"
              :orders {:number 3}}}])


(insert! env DATA)

(select env {:account/user '_}
        :access {:account {:orders :checked}}
        :ids false)

(def cid (select-ids env {:account/user "Chris"}))
(update! env {:account/user "Chris"} {:account/user "John"} :transact :compare :ids)
(def jid (select-ids env {:account/user "John"}))
(assert (= cid jid))

(delete! env #{:account/user :order/number})

(def env1
  (sync-> env [:simulate]
          (transact! [{:db/id (iid) :account/user "Chris"}])
          (transact! [{:db/id (iid) :account/user "Chris"}])
          ))

(fact "Simulations"
  (-> env
      (insert! (vec (concat DATA DATA)) :simulate)
      (select-ids :order/number))
  => (four-of number?))

(fact "Have no effect on actual collection"
  (select-ids env {:account/user "Chris"})
  => #{})

;; This time, update the env to the db

(def env1 (insert! env DATA :simulate))

(fact "select-ids"
  (select-ids env1 {:account/user "Chris"})
  => (one-of number?)

  (select-ids env1 {:account/user "Chris"})
  => (one-of number?)

  (select-ids env1 {:account/user '(= "Chris")})
  => (one-of number?)

  (select-entities env1 {:account/user '(= "Chris")})
  => (one-of #(instance? datomic.Entity %))

  (select-entities env1 {:account/user '(= "Chris")}
                   :model {:allow {}})
  => (raises-issue {:adi true :normalise true :not-allowed true})

  (select env1 {:account/user '(= "Chris")}
          :model {:return {:account/user :checked}})

  (select env1 {:account {:user [["(= \"Chris\")"]]}}
          :access {:account {:orders :checked}})
  => #{{:account {:user "Chris", :orders #{{:number 2}}}}}

  (select env1 {:account/user "Hello"}
          :access {:account {:orders :checked}})
  => #{{:account {:user "Hello", :orders #{{:number 1}}}}}

  (select env1 {:account/user '_}
          :access {:account {:orders :checked}}
          :ids false)
  => #{{:account {:user "Chris", :orders #{{:number 2}}}}
       {:account {:user "Hello", :orders #{{:number 1}}}}})

(q env1 '[:find ?x :where
          [?x :account/user _]] :first)

(selectq-entities env1 '[:find ?x :where
                          [?x :account/user _]])

(selectq env1 '[:find ?x :where
                 [?x :account/user _]]
         :access {:account {:orders :checked}})

(selectq env1 '[:find ?x :where
                [?x :order/account _]]
          :access {:order {:account :checked}})

(selectq-ids env1 '[:find ?x :where
                 [?x :account/user _]])

(comment
  (insert! env DATA)

  #_(fact
      (retract! env '{:account/user _}
                #{:account/orders/number}
                :access {:account {:orders :checked}}
                :raw)
      (retract! env '{:account/user _}
                #{:account/orders/number}
                :access {:account :checked}
                :transact :compare)
      )

  (update-in! env {:account/user '_}
              [:account/orders {:number '(= 2)}]
              {:number 3} :raw)

  (delete-in! env {:account/user '_}
              [:account/orders {:number '_}] :raw)

  (fact (retract-in! env {:account/user '_}
                     [:account/orders {:number '_}]
                     #{:number} :raw) => nil)

  (search-path-analysis
   [[:account/orders {:number 1}]
    [:items {:name "crystal"}]
    [:images {:url "./image1.png"}]]
   (-> env :schema :tree))


  (build-search-term
   1000
   [{:term {:number 1}, :ns :order, :rval :accounts, :val :orders}
    {:term {:name "crystal"}, :ns :orderItem, :rval :order, :val :items}
    {:term {:url "./image1.png"}, :ns :image, :rval :orderItems, :val :images}])


  ((build-search-term-fn
    [{:term 19999321, :ns :order, :rval :accounts, :val :orders}
     {:term {:name "crystal"}, :ns :orderItem, :rval :order, :val :items}
     {:term {:url "./image1.png"}, :ns :image, :rval :orderItems, :val :images}])
   10002323)

  (def DATA2
    {:image {:url "./image1.png",
             :orderItems
             {:name "crystal",
              :order {:number 10
                      :accounts {:user "chris"}}}}})

  (insert! env DATA2)

  (def imodel (am/model-input {:account {:orders {:items :checked}}} (-> env :schema :tree)))
  (def rmodel (am/model-unpack imodel (-> env :schema :tree)))

  (def imodel2 (am/model-input {:image {:orderItems {:order {:accounts :checked}} }} (-> env :schema :tree)))
  (def rmodel2 (am/model-unpack imodel (-> env :schema :tree)))

  rmodel2
  {:account/tags :checked,
   :account/user :checked
   :account/orders
   {:order/accounts :unchecked,
    :order/items {:orderItem/order :unchecked,
                  :orderItem/images :unchecked,
                  :orderItem/name :checked},
    :order/number :checked}}


  (select env {:image/url '_}
          :access {:image {:orderItems {:order {:accounts :checked}}}})

  (select env {:account/user '_}
          :access {:account {:orders {:items {:images :checked}}}} :ids)

  (fact
    (delete-all! env {:account/user '_}
                 :access {:account {:orders {:items :checked}}} :raw)

    (linked-entities (d/entity (d/db (:conn env)) 17592186045433)
                     rmodel2
                     env)
    => nil)


  (fact
    (update-in! env
                {:account/user '_}
                [:account/orders '_
                 :items  '_]
                {:name "hello"} :raw)
    => nil)
)
