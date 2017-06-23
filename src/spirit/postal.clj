(ns spirit.postal
  (:require [spirit.protocol.imail :as mail]
            [spirit.common.mustache :as stencil]
            [spirit.common.mail :as common]))


(defrecord PostalMailer []

  mail/IMailer
  #_(-send-mail [mailer {:keys []} data]))


(defmethod common/mailer :postal
  [m]
  (map->PostalMailer m))

(comment
  (require '[lucid.package])
  (lucid.package/pull '[com.draines/postal "2.0.2"])

  
  (stencil/to-html (spirit.common.mustache.Mustache/preprocess "{{user.name}}")
                   {:user.name "chris"}) 
  
  (def postal (map->PostalMailer {:type :postal
                                  :host "smtp.webfaction.com"
                                  :port 465
                                  :user "test_keynect"
                                  :pass "keynect"
                                  :ssl true
                                  :defaults {:from "z@caudate.me"}}))
  
  (mailer/-send postal {:message {:to "{{user.name}}"
                                  :cc ""
                                  :title "{{title}}"}
                        :data {:user }
                        })

  
  ()
  
  (require '[postal.core :as email])

  (email/send-message mailer
                      {})

  (require '[postal.sendmail :as send])
  (send/sendmail-send {:from "z@caudate.me"
                       :to ["z@caudate.me" "zcaudate@gmail.com"]
                       :cc "z@caudate.me"
                       :bcc "z@caudate.me"
                       :subject "Hi!"
                       :body "Test"})
  
  (email/send-message {:from "z@caudate.me"
                       :to "zcaudate@gmail.com"
                       :cc "z@caudate.me"
                       :subject "Hi!"
                       :body "Test."})
  
  (email/send-message
   {:type :postal
    :host "smtp.webfaction.com"
    :port 465
    :user "test_keynect"
    :pass "keynect"
    :ssl true}
   {:from "z@caudate.me"
    :to ["z@caudate.me" "zcaudate@gmail.com"]
    
    :subject "Hi!"
    :body "Test."
    })
  )
