(ns spirit.datomic-test
  (:use hara.test)
  (:require [spirit.datomic :refer :all]))


(comment
  (def ds (connect! {:type     :datomic
                     :protocol :mem
                     :name "spirit-test"}
                    {:account {:user [{:required true}]}}
                    true true))
  
  (insert! ds {:account/user "Hello"} :raw)
  
  (insert! ds {:account {:user "Heeuello"}} :debug)
  
  (insert! ds [{:account/user "A"} {:account/user "B"} {:account/user "C"}])
  
  (select ds :account :ids)
  )