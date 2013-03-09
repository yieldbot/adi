(ns adi.data.scratch
  (:use midje.sweet
        adi.utils
        adi.checkers)
  (:require [adi.data :as ad]))


(def game-map
  (flatten-all-keys
   {:game {:name  [{:type    :string
                    :required true}]
           :score [{:type    :long
                    :default 0}]}
    :profile {:avatar [{:type    :keyword
                        :default :human}]}}))

(fact
  (ad/process {:profile {} :game {}} game-map
              {:defaults? true
               :nss #{:profile :game}})
  => {:profile/avatar :human
      :game/score 0}
)