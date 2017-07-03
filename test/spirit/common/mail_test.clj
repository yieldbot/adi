(ns spirit.common.mail-test
  (:use hara.test)
  (:require [spirit.common.mail :refer :all]))

^{:refer spirit.common.mail/send-mail :added "0.5"}
(fact "sends mail to the following mailbox"

  (-> (doto  (mail {:type :mock
                    :file "test.mailbox.db"})
        (send-mail "z@caudate.me" nil {:a 1 :b 2})
        (send-mail "z@caudate.me" nil {:x 1 :y 2})
        (send-mail "z@caudate.me" nil {:a 1 :b 2}))
      (list-mail))
  => {"z@caudate.me" [{:a 1, :b 2}
                      {:x 1, :y 2}
                      {:a 1, :b 2}]})

^{:refer spirit.common.mail/list-mail :added "0.5"}
(fact "lists all mail sent to the mailbox"
  
  (-> (doto (mail {:type :mock})
        (send-mail "z@caudate.me" nil {:a 1 :b 2}))
      (list-mail))
  => {"z@caudate.me" [{:a 1, :b 2}]})

^{:refer spirit.common.mail/clear-mail :added "0.5"}
(fact "clears all mail sent to the mailbox"

  (-> (doto (mail {:type :mock})
        (send-mail "z@caudate.me" nil {:a 1 :b 2})
        (clear-mail))
      (list-mail))
  => {})

^{:refer spirit.common.mail/create :added "0.5"}
(fact "creates a mail service"

  (create {:type :raw})

  (create {:type :mock
           :file "test.mail.db"}))

^{:refer spirit.common.mail/mail :added "0.5"}
(fact "creates a mail service"
  
  (mail {:type :mock}))
