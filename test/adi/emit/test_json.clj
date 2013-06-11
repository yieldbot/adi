(ns adi.emit.test-json
  (:use midje.sweet)
  (:require [adi.emit.datoms :as dat]))

(comment
  (defn emit-datoms-insert* [data env]
    (dat/emit-datoms data env {:generate {:ids {:current true}}
                               :options {:extras? true
                                         :json? true}}))

  (emit-datoms-insert*
   {"lastName" "Zheng", "business" {"name" "Crystal Universe", "locale" "australia", "abn" "934829378984", "industry" ["new-age"], "desc" "Wholesale Crystals, Minerals and Gems", "contacts" {"field" {"phone" "0415804201"}}, "addresses" {"line1" "Unit 6, 297 Ingles St", "city" "Port Melbourne", "postcode" 3217, "region" "Victoria", "country" "US"}}, "passwordc" "zcauda", "emailc" "z@caudate.com", "firstName" "Chris", "username" "zcaud", "email" "z@caudate.com", "password" "zcauda"})
)
