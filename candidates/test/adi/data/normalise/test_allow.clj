(ns adi.data.normalise.test-allow
  (:use midje.sweet)
  (:require
   [adi-test.schemas :refer :all]
   [adi-test.checkers :refer :all]
   [hara.collection.hash-map :refer [treeify-keys]]
   [adi.model :refer [model-input]]
   [adi.data.normalise :as t :refer [normalise]]))


(fact "Simple Allow"
  (t/normalise {:account/name "Chris"}
               {:schema account-name-age-sex-xm
                :model {:allow {}}})
  => (raises-issue {:adi true, 
                    :data {:name "Chris"}, 
                    :key-path [:account], 
                    :normalise true, 
                    :not-allowed true, 
                    :nsv [:account]})

  (t/normalise {:account/name "Chris"}
               {:schema account-name-age-sex-xm
                :model {:allow {:account {:name :checked}}}})
  => {:account {:name "Chris"}}

  (t/normalise {:account {:name "Chris"
                          :age 10}}
               {:schema account-name-age-sex-xm
                :model {:allow {:account {:name :checked}}}})
  => (raises-issue {:adi true, 
                    :data 10, 
                    :key-path [:account :age], 
                    :normalise true, 
                    :not-allowed true, 
                    :nsv [:account :age]}))

(fact "Allow with Refs"
 (t/normalise {:account/orders/number 1}
              {:schema account-orders-items-image-xm
               :model {:allow {:account {:orders {:number :checked}}}}})
 => {:account {:orders #{{:number 1}}}}

 (t/normalise {:account {:user "Chris"}}
              {:schema account-orders-items-image-xm
               :model {:allow {:account {:user :checked}}}})
 => {:account {:user "Chris"}}
 
 

 (t/normalise {:account {:orders {:+ {:account {:user "Chris"}}}}}
              {:schema account-orders-items-image-xm
               :model {:allow {:account {:user :checked
                                         :orders {:+ {:account {:user :checked}}}}}}})
 => {:account {:orders #{{:+ {:account {:user "Chris"}}}}}}
 

 (t/normalise {:account {:orders {:+ {:account {:user "Chris"}}}}}
              {:schema account-orders-items-image-xm
               :model {:allow {:account {:user :checked
                                         :orders {:+ {:order {:number :checked}}}}}}})
 => (throws)
 )