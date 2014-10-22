(ns adi.data.test-characterise
  (:use midje.sweet)
  (:require
   [adi-test.schemas :refer :all]
   [adi-test.checkers :refer :all]
   [adi.common :refer :all]
   [adi.data.normalise :refer [normalise]]
   [adi.data.pack.analyse :refer [analyse]]
   [adi.data.characterise :refer [characterise]]))

(defn ->characterise [data env]
  (-> (normalise data env)
      (analyse env)
      (characterise env)))

(fact
  (->characterise {:account/name "Chris"}
                  {:schema account-name-age-sex-xm})
  => {:data-one {:account/name "Chris"}}

  (->characterise {:account/name "Chris"}
                  {:schema account-name-age-sex-xm
                   :type "query"})
  => {:data-many {:account/name #{"Chris"}}, :# {:sym '?self}}

  (->characterise {:db/id '?self
                   :account/name "Chris"}
                  {:schema account-name-age-sex-xm
                   :type "datoms"})
  => {:data-one {:account/name "Chris"}, :# {:id #db/id[:db.part/user -1994032528]}}


  (->characterise {:account/name '?x}
                  {:schema account-name-age-sex-xm
                   :type "query"
                   :options {:generate-syms false}})
  => {:data-many {:account/name #{'?x}}, :# {:sym '?self}}


  (->characterise {:account/orders '?x}
                  {:schema account-orders-items-image-xm
                   :type "datoms"
                   :options {:generate-ids (incremental-id-gen)}})
  => {:rev-ids {:order/account #{#db/id[:db.part/user -1640396314]}},
      :rev-ids-many {:order/account #{}}, :# {:id 1}}

  (->characterise {:account/orders '#{?x ?y}}
                  {:schema account-orders-items-image-xm
                   :type "datoms"
                   :options {:generate-ids (incremental-id-gen)}})
  => {:rev-ids {:order/account #{#db/id[:db.part/user -1640396314] #db/id[:db.part/user -1640396251]}},
      :rev-ids-many {:order/account #{}}, :# {:id 1}}

  (->characterise {:account/orders '#{?y ?x}}
                  {:schema account-orders-items-image-xm
                   :type "query"
                   :options {:generate-syms (incremental-sym-gen "e")}})
  => {:rev-ids {:order/account #{'?y '?x}}, :rev-ids-many {:order/account #{}}, :# {:sym '?self}})


(->characterise {:account
                 {:user "Hello"
                  :orders {:number 1
                           :account {:user "Chris"
                                     :orders {:number 2}}}}}
                {:schema account-orders-items-image-xm
                 :type "datoms"})
{:data-one {:account/user "Hello"}, :# {:id #db/id[:db.part/user -1000228]}
 :revs-many
 {:order/account #{{:data-one {:order/number 1}, :# {:rkey :order/account, :rid #db/id[:db.part/user -1000228], :id #db/id[:db.part/user -1000229]}
                    :refs-one
                    {:order/account
                     {:revs-many {:order/account #{{:data-one {:order/number 2}, :# {:rkey :order/account, :rid #db/id[:db.part/user -1000230], :id #db/id[:db.part/user -1000231]}}}}, :data-one {:account/user "Chris"}, :# {:id #db/id[:db.part/user -1000230]}}


                     }}}},
 }

(->characterise {:account
                 {:user "Hello"
                  :orders {:number 1
                           :account {:user "Chris"}}}}
                {:schema account-orders-items-image-xm
                 :type "datoms"})
{:data-one {:account/user "Hello"}, :# {:id #db/id[:db.part/user -1000239]}
 :revs-many {:order/account #{{:data-one {:order/number 1},
                               :# {:rkey :order/account,
                                   :rid #db/id[:db.part/user -1000239],
                                   :id #db/id[:db.part/user -1000240]}
                               :refs-one {:order/account {:data-one {:account/user "Chris"},
                                                          :# {:id #db/id[:db.part/user -1000241]}}},
                               }}}}

[{:db/id #db/id[:db.part/user -1000239] :account/user "Hello"}
 {:db/id #db/id[:db.part/user -1000240] :order/number 1}
 {:db/id #db/id[:db.part/user -1000239] :order/account #db/id[:db.part/user -1000240]}
 {:db/id #db/id[:db.part/user -1000240]
  :order/account {:db/id #db/id[:db.part/user -1000241] :account/user "Chris"}}]
